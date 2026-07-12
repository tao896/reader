<template>
  <div class="hot-search-wrapper" v-show="visible">
    <div class="hot-search-toolbar">
      <div class="hot-search-platforms">
        <button
          v-for="platform in platforms"
          :key="platform.platformId"
          class="platform-btn"
          :class="{ active: currentPlatform === platform.platformId }"
          @click="selectPlatform(platform.platformId)"
        >
          {{ platform.platformName }}
        </button>
      </div>
      <div class="toolbar-actions">
        <button
          v-if="currentPlatform === 'weibo'"
          class="toolbar-btn"
          @click="openUrl('https://weibo.com/hot/mine')"
          title="打开微博网页版我的热搜"
        >
          打开我的热搜
        </button>
        <button
          v-if="currentPlatform === 'weibo'"
          class="toolbar-btn"
          :class="{ configured: weiboSessionConfigured }"
          @click="showWeiboSessionDialog = true"
        >
          {{ weiboSessionConfigured ? "更新微博登录" : "配置微博登录" }}
        </button>
        <button
          v-if="currentPlatform === 'weibo' && weiboSessionConfigured"
          class="toolbar-btn danger"
          @click="clearWeiboSession"
        >
          清除登录
        </button>
        <button
          class="toolbar-btn refresh-btn"
          :disabled="loading"
          @click="refresh"
        >
          <i class="el-icon-refresh" :class="{ rotating: loading }"></i>
          刷新
        </button>
      </div>
    </div>

    <div class="hot-search-meta" v-if="fetchedAt && !loading">
      <span>更新于 {{ formatTime(fetchedAt) }}</span>
      <span
        class="source-badge"
        :class="{ aggregated: source === 'aggregated' }"
      >
        {{ sourceName }}
      </span>
      <span class="stale-badge" v-if="stale">缓存数据</span>
      <span
        v-if="currentPlatform === 'weibo' && weiboSessionExpiresAt"
        class="session-expiry"
      >
        登录配置有效至 {{ formatSessionExpiry(weiboSessionExpiresAt) }}
      </span>
    </div>

    <div class="hot-search-list">
      <div class="hot-search-skeleton" v-if="loading && !items.length">
        <div class="skeleton-row" v-for="n in 10" :key="n">
          <span></span>
          <div><i></i><i></i></div>
        </div>
      </div>

      <div class="hot-search-error" v-else-if="errorMsg && !items.length">
        <p>{{ errorMsg }}</p>
        <button
          v-if="currentPlatform === 'weibo' && !weiboSessionConfigured"
          @click="showWeiboSessionDialog = true"
        >
          配置微博登录
        </button>
        <button v-else @click="loadHotSearches(false)">重试</button>
      </div>

      <div class="hot-search-empty" v-else-if="!loading && !items.length">
        暂无热搜数据
      </div>

      <template v-else>
        <article
          class="hot-search-item"
          :class="{ 'has-thumbnail': item.imageUrl }"
          v-for="item in items"
          :key="currentPlatform + '-' + item.rank + '-' + item.title"
          @click="openUrl(item.url)"
        >
          <div class="rank-number" :class="rankClass(item.rank)">
            {{ item.rank }}
          </div>
          <img
            class="hot-thumbnail"
            v-if="item.imageUrl"
            :src="item.imageUrl"
            alt=""
            loading="lazy"
          />
          <div class="hot-content">
            <div class="hot-title-row">
              <h3>{{ item.title }}</h3>
              <span class="hot-label" v-if="item.label">{{ item.label }}</span>
            </div>
            <p v-if="item.summary">{{ item.summary }}</p>
            <div class="hot-value" v-if="item.hotValue">
              <i class="el-icon-data-line"></i>
              {{ item.hotValue }}
            </div>
          </div>
          <button
            class="open-btn"
            title="打开原站"
            @click.stop="openUrl(item.url)"
          >
            ↗
          </button>
        </article>
      </template>

      <div class="refreshing-tip" v-if="loading && items.length">
        <i class="el-icon-loading"></i> 正在刷新...
      </div>
    </div>

    <el-dialog
      title="配置微博登录"
      :visible.sync="showWeiboSessionDialog"
      :width="weiboDialogWidth"
      append-to-body
      :close-on-click-modal="false"
      @closed="clearWeiboCookieInput"
    >
      <div class="weibo-session-help">
        <p>
          用于读取微博网页版“我的”热搜。Cookie 仅保存在当前 Reader
          会话的服务端内存中，最长 12 小时，重启或清除后立即失效。
        </p>
        <ol>
          <li>
            点击“打开我的热搜”，在微博网页完成登录。
          </li>
          <li>
            打开浏览器开发者工具的 Network，刷新页面并选择
            <code>mineBand</code> 请求。
          </li>
          <li>
            在 Request Headers 中复制完整的 <code>Cookie</code> 值并粘贴到下方。
          </li>
        </ol>
        <div class="cookie-warning">
          Cookie 等同于微博登录凭据，请只在本机或通过 HTTPS 访问的可信 Reader
          服务上配置，不要发送给其他人。
        </div>
      </div>
      <el-input
        v-model="weiboCookieInput"
        type="textarea"
        :rows="6"
        resize="vertical"
        autocomplete="off"
        spellcheck="false"
        placeholder="例如：SUB=...; SUBP=...; XSRF-TOKEN=..."
      ></el-input>
      <span slot="footer" class="dialog-footer">
        <el-button @click="showWeiboSessionDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="savingWeiboSession"
          :disabled="!weiboCookieInput.trim()"
          @click="saveWeiboSession"
        >
          验证并保存到内存
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import Axios from "../plugins/axios";

export default {
  name: "HotSearch",
  props: {
    visible: { type: Boolean, default: false },
    api: { type: String, default: "" }
  },
  data() {
    return {
      platforms: [],
      currentPlatform: "",
      items: [],
      loading: false,
      errorMsg: "",
      source: "",
      sourceName: "",
      fetchedAt: 0,
      stale: false,
      optionsLoaded: false,
      lastRequestId: 0,
      weiboSessionConfigured: false,
      weiboSessionExpiresAt: 0,
      showWeiboSessionDialog: false,
      weiboCookieInput: "",
      savingWeiboSession: false
    };
  },
  computed: {
    weiboDialogWidth() {
      return window.innerWidth <= 600 ? "92%" : "560px";
    }
  },
  watch: {
    visible(value) {
      if (value && !this.optionsLoaded) this.loadOptions();
    },
    api() {
      this.optionsLoaded = false;
      this.platforms = [];
      this.currentPlatform = "";
      this.applyWeiboSessionStatus(null);
      if (this.visible) this.loadOptions();
    }
  },
  methods: {
    loadOptions() {
      if (!this.api) return;
      this.errorMsg = "";
      Axios.get(this.api + "/getHotSearchOptions").then(
        response => {
          if (!response.data.isSuccess) {
            this.errorMsg = response.data.errorMsg || "获取热搜平台失败";
            return;
          }
          const data = response.data.data || {};
          this.platforms = data.platforms || [];
          this.applyWeiboSessionStatus(data.weiboSession);
          this.optionsLoaded = true;
          const platform =
            data.defaultPlatform ||
            (this.platforms[0] && this.platforms[0].platformId);
          if (platform) this.selectPlatform(platform);
        },
        () => {
          this.errorMsg = "获取热搜平台失败，请检查后端连接";
        }
      );
    },
    selectPlatform(platformId) {
      if (this.currentPlatform === platformId && this.items.length) return;
      this.currentPlatform = platformId;
      this.items = [];
      this.source = "";
      this.sourceName = "";
      this.fetchedAt = 0;
      this.stale = false;
      if (platformId === "weibo" && !this.weiboSessionConfigured) {
        this.errorMsg = "请先配置微博登录 Cookie，以读取“我的”热搜";
        return;
      }
      this.loadHotSearches(false);
    },
    loadHotSearches(forceRefresh) {
      if (!this.api || !this.currentPlatform) return;
      if (this.currentPlatform === "weibo" && !this.weiboSessionConfigured) {
        this.errorMsg = "请先配置微博登录 Cookie，以读取“我的”热搜";
        return;
      }
      this.loading = true;
      this.errorMsg = "";
      const requestId = ++this.lastRequestId;
      const params = new URLSearchParams();
      params.set("platform", this.currentPlatform);
      if (forceRefresh) params.set("refresh", "true");

      Axios.get(this.api + "/getHotSearches?" + params.toString(), {
        timeout: 30000
      }).then(
        response => {
          if (requestId !== this.lastRequestId) return;
          this.loading = false;
          if (!response.data.isSuccess) {
            this.errorMsg = response.data.errorMsg || "加载热搜失败";
            if (this.currentPlatform === "weibo") {
              this.loadWeiboSessionStatus();
            }
            return;
          }
          const data = response.data.data || {};
          this.items = data.items || [];
          this.source = data.source || "";
          this.sourceName = data.sourceName || "";
          this.fetchedAt = data.fetchedAt || 0;
          this.stale = Boolean(data.stale);
        },
        error => {
          if (requestId !== this.lastRequestId) return;
          this.loading = false;
          this.errorMsg = "请求失败: " + (error && error.toString());
        }
      );
    },
    refresh() {
      if (!this.loading) this.loadHotSearches(true);
    },
    loadWeiboSessionStatus() {
      if (!this.api) return;
      Axios.get(this.api + "/getWeiboHotSearchSession").then(response => {
        if (response.data.isSuccess) {
          this.applyWeiboSessionStatus(response.data.data);
        }
      });
    },
    applyWeiboSessionStatus(status) {
      const value = status || {};
      this.weiboSessionConfigured = Boolean(value.configured);
      this.weiboSessionExpiresAt = value.expiresAt || 0;
    },
    saveWeiboSession() {
      const cookie = this.weiboCookieInput.trim();
      if (!cookie || this.savingWeiboSession) return;
      this.savingWeiboSession = true;
      Axios.post(
        this.api + "/setWeiboHotSearchSession",
        { cookie },
        { silent: true }
      ).then(
        response => {
          this.savingWeiboSession = false;
          this.clearWeiboCookieInput();
          if (!response.data.isSuccess) {
            this.$message.error(response.data.errorMsg || "验证微博登录失败");
            return;
          }
          this.applyWeiboSessionStatus(response.data.data);
          this.showWeiboSessionDialog = false;
          this.$message.success("微博登录配置成功");
          if (this.currentPlatform === "weibo") {
            this.items = [];
            this.loadHotSearches(false);
          }
        },
        error => {
          this.savingWeiboSession = false;
          this.clearWeiboCookieInput();
          this.$message.error(
            "验证微博登录失败: " + (error && error.toString())
          );
        }
      );
    },
    clearWeiboSession() {
      this.$confirm(
        "清除后需要重新配置 Cookie 才能读取微博“我的”热搜，是否继续？",
        "清除微博登录",
        { type: "warning" }
      )
        .then(() =>
          Axios.post(
            this.api + "/clearWeiboHotSearchSession",
            {},
            { silent: true }
          )
        )
        .then(response => {
          if (!response || !response.data.isSuccess) return;
          this.applyWeiboSessionStatus(response.data.data);
          this.items = [];
          this.errorMsg = "请先配置微博登录 Cookie，以读取“我的”热搜";
          this.$message.success("微博登录已从内存中清除");
        })
        .catch(error => {
          if (error && error !== "cancel" && error !== "close") {
            this.$message.error("清除微博登录失败: " + error.toString());
          }
        });
    },
    clearWeiboCookieInput() {
      this.weiboCookieInput = "";
    },
    openUrl(url) {
      if (!url) return;
      const opened = window.open(url, "_blank", "noopener,noreferrer");
      if (opened) opened.opener = null;
    },
    rankClass(rank) {
      if (rank === 1) return "top-one";
      if (rank === 2) return "top-two";
      if (rank === 3) return "top-three";
      return "";
    },
    formatTime(timestamp) {
      const date = new Date(timestamp);
      const pad = value => (value < 10 ? "0" + value : value);
      return pad(date.getHours()) + ":" + pad(date.getMinutes());
    },
    formatSessionExpiry(timestamp) {
      const date = new Date(timestamp);
      const pad = value => (value < 10 ? "0" + value : value);
      return (
        pad(date.getMonth() + 1) +
        "-" +
        pad(date.getDate()) +
        " " +
        pad(date.getHours()) +
        ":" +
        pad(date.getMinutes())
      );
    }
  }
};
</script>

<style lang="stylus" scoped>
.hot-search-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.hot-search-toolbar {
  padding: 12px 16px 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}

.hot-search-platforms,
.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.platform-btn,
.toolbar-btn {
  border: 1px solid var(--modern-line, #e4e7ed);
  background: transparent;
  color: var(--modern-muted, #606266);
  cursor: pointer;
  transition: all 160ms ease;
}

.platform-btn {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 14px;

  &:hover,
  &.active {
    border-color: var(--modern-accent, #4f6ef7);
    color: var(--modern-accent, #4f6ef7);
  }

  &.active {
    color: #fff;
    background: var(--modern-accent, #4f6ef7);
  }
}

.toolbar-btn {
  padding: 5px 10px;
  border-radius: 6px;
  font-size: 12px;

  &:hover:not(:disabled) {
    border-color: var(--modern-accent, #4f6ef7);
    color: var(--modern-accent, #4f6ef7);
  }

  &:disabled {
    opacity: .55;
    cursor: default;
  }

  &.configured {
    border-color: #55a878;
    color: #3b8f60;
  }

  &.danger {
    color: #d65b52;
  }
}

.rotating {
  animation: hot-rotate 1s linear infinite;
}

.hot-search-meta {
  min-height: 22px;
  padding: 0 16px 7px;
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--modern-weak, #909399);
  font-size: 11px;
  flex-shrink: 0;
}

.source-badge,
.stale-badge,
.hot-label {
  padding: 1px 6px;
  border-radius: 9px;
}

.source-badge {
  color: #3b8f60;
  background: rgba(50, 180, 110, .1);

  &.aggregated {
    color: #b87820;
    background: rgba(230, 162, 60, .13);
  }
}

.stale-badge {
  color: #b87820;
  background: rgba(230, 162, 60, .13);
}

.session-expiry {
  margin-left: auto;
}

.hot-search-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 18px;
}

.hot-search-item {
  min-height: 76px;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 30px;
  align-items: center;
  gap: 12px;
  padding: 11px 6px;
  border-bottom: 1px solid var(--modern-line, #f0f0f0);
  cursor: pointer;
  transition: background 150ms ease;

  &:hover {
    background: var(--modern-soft, #fafafa);
    border-radius: 8px;
  }

  &.has-thumbnail {
    grid-template-columns: 34px 92px minmax(0, 1fr) 30px;
  }
}

.rank-number {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  color: var(--modern-muted, #909399);
  background: var(--modern-soft, #f4f4f5);
  font-weight: 700;
  font-size: 13px;

  &.top-one { color: #fff; background: linear-gradient(135deg, #ff6434, #f13c2f); }
  &.top-two { color: #fff; background: linear-gradient(135deg, #ffad32, #f08720); }
  &.top-three { color: #fff; background: linear-gradient(135deg, #5a9bf8, #3976dc); }
}

.hot-thumbnail {
  width: 92px;
  height: 58px;
  border-radius: 6px;
  object-fit: cover;
  background: var(--modern-soft, #f4f4f5);
}

.hot-content {
  min-width: 0;
}

.hot-title-row {
  display: flex;
  align-items: center;
  gap: 7px;

  h3 {
    min-width: 0;
    margin: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--modern-text, #303133);
    font-size: 14px;
    font-weight: 600;
  }
}

.hot-label {
  flex-shrink: 0;
  color: #e85847;
  background: rgba(232, 88, 71, .1);
  font-size: 10px;
}

.hot-content p {
  margin: 5px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--modern-weak, #909399);
  font-size: 12px;
}

.hot-value {
  margin-top: 5px;
  color: #e66b4b;
  font-size: 11px;
}

.open-btn {
  width: 28px;
  height: 28px;
  border: 1px solid var(--modern-line, #e4e7ed);
  border-radius: 6px;
  background: transparent;
  color: var(--modern-muted, #909399);
  cursor: pointer;

  &:hover {
    border-color: var(--modern-accent, #4f6ef7);
    color: var(--modern-accent, #4f6ef7);
  }
}

.hot-search-error,
.hot-search-empty {
  padding: 70px 20px;
  text-align: center;
  color: var(--modern-weak, #909399);

  button {
    padding: 7px 22px;
    border: 1px solid var(--modern-line, #e4e7ed);
    border-radius: 15px;
    background: transparent;
    color: var(--modern-muted, #606266);
    cursor: pointer;
  }
}

.refreshing-tip {
  padding: 14px;
  text-align: center;
  color: var(--modern-muted, #909399);
  font-size: 12px;
}

.weibo-session-help {
  color: #606266;
  font-size: 13px;
  line-height: 1.7;

  p {
    margin: 0 0 10px;
  }

  ol {
    margin: 0 0 12px;
    padding-left: 22px;
  }

  code {
    padding: 1px 4px;
    border-radius: 3px;
    background: #f3f4f6;
    color: #c44b40;
  }
}

.cookie-warning {
  margin-bottom: 14px;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(230, 162, 60, .12);
  color: #a36c18;
}

.skeleton-row {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 12px;
  padding: 15px 6px;
  border-bottom: 1px solid var(--modern-line, #f0f0f0);

  > span,
  i {
    display: block;
    border-radius: 5px;
    background: var(--modern-soft, #f4f4f5);
    animation: hot-pulse 1.5s ease-in-out infinite;
  }

  > span { width: 28px; height: 28px; }
  div { display: flex; flex-direction: column; gap: 8px; }
  i:first-child { width: 62%; height: 15px; }
  i:last-child { width: 38%; height: 11px; }
}

@keyframes hot-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: .4; }
}

@keyframes hot-rotate {
  from { transform: rotate(0); }
  to { transform: rotate(360deg); }
}

@media (max-width: 600px) {
  .hot-search-toolbar { align-items: flex-start; }
  .hot-search-item,
  .hot-search-item.has-thumbnail { grid-template-columns: 30px minmax(0, 1fr) 28px; gap: 8px; }
  .hot-thumbnail { display: none; }
  .hot-content p { white-space: normal; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
}

.night & {
  .platform-btn,
  .toolbar-btn,
  .open-btn { border-color: #3a3a3a; color: #aaa; }
  .platform-btn.active { color: #fff; border-color: var(--modern-accent, #4f6ef7); }
  .hot-search-item:hover { background: #2a2a2a; }
  .hot-title-row h3 { color: #eee; }
  .rank-number:not(.top-one):not(.top-two):not(.top-three),
  .skeleton-row > span,
  .skeleton-row i { background: #333; }
}
</style>
