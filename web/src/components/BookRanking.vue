<template>
  <div class="ranking-wrapper" v-show="visible">
    <div class="ranking-header">
      <div class="ranking-sites">
        <button
          v-for="site in sites"
          :key="site.siteId"
          class="ranking-site-btn"
          :class="{ active: currentSite === site.siteId }"
          @click="selectSite(site.siteId)"
        >
          {{ site.siteName }}
        </button>
      </div>
    </div>
    <div class="ranking-filters" v-if="currentSiteConfig">
      <div class="filter-group">
        <button
          v-for="rt in currentSiteConfig.rankTypes"
          :key="rt.id"
          class="filter-btn"
          :class="{ active: currentRankType === rt.id }"
          @click="selectRankType(rt.id)"
        >
          {{ rt.name }}
        </button>
      </div>
      <div
        class="filter-group"
        v-for="filter in currentFilters"
        :key="filter.id"
      >
        <button
          v-for="opt in filter.options"
          :key="opt.id"
          class="filter-btn small"
          :class="{ active: filterSelections[filter.id] === opt.id }"
          @click="selectFilter(filter.id, opt.id)"
        >
          {{ opt.name }}
        </button>
      </div>
    </div>
    <div class="ranking-meta" v-if="fetchedAt && !loading">
      <span class="ranking-time">更新于 {{ formatTime(fetchedAt) }}</span>
      <span class="ranking-stale" v-if="stale">（缓存数据）</span>
    </div>
    <div class="ranking-list" ref="rankingList" @scroll="handleScroll">
      <div class="ranking-skeleton" v-if="loading && !books.length">
        <div class="skeleton-item" v-for="n in 8" :key="n">
          <div class="skeleton-rank"></div>
          <div class="skeleton-cover"></div>
          <div class="skeleton-info">
            <div class="skeleton-line w60"></div>
            <div class="skeleton-line w40"></div>
            <div class="skeleton-line w80"></div>
          </div>
        </div>
      </div>
      <div
        class="ranking-empty"
        v-else-if="!loading && !books.length && !errorMsg"
      >
        暂无排行数据
      </div>
      <div class="ranking-error" v-else-if="errorMsg && !books.length">
        <p>{{ errorMsg }}</p>
        <button class="retry-btn" @click="loadRankings(1)">重试</button>
      </div>
      <template v-else>
        <div
          class="ranking-card"
          v-for="book in books"
          :key="book.siteBookId || book.name + book.rank"
          @click="handleBookClick(book)"
        >
          <div class="rank-badge" :class="getRankClass(book.rank)">
            {{ book.rank }}
          </div>
          <div class="rank-cover">
            <el-image
              :src="getBookCover(book.coverUrl)"
              fit="cover"
              lazy
            ></el-image>
          </div>
          <div class="rank-info">
            <div class="rank-name">{{ book.name }}</div>
            <div class="rank-author">{{ book.author }}</div>
            <div class="rank-meta-row">
              <span class="rank-category" v-if="book.category">{{
                book.category
              }}</span>
              <span class="rank-metric" v-if="book.metric">{{
                book.metric
              }}</span>
            </div>
            <div class="rank-intro" v-if="book.intro">{{ book.intro }}</div>
            <div class="rank-latest" v-if="book.latestChapter">
              最新：{{ book.latestChapter }}
            </div>
          </div>
          <div class="rank-actions">
            <button
              class="official-link-btn"
              v-if="book.officialUrl"
              @click.stop="openOfficial(book.officialUrl)"
              title="打开官方页"
            >
              ↗
            </button>
          </div>
        </div>
      </template>
      <div class="ranking-load-more" v-if="books.length && !loading">
        <button class="load-more-btn" :disabled="!hasMore" @click="loadMore">
          {{ hasMore ? "加载更多" : "没有更多了" }}
        </button>
      </div>
      <div class="ranking-loading-more" v-if="loading && books.length">
        <i class="el-icon-loading"></i> 加载中...
      </div>
    </div>
  </div>
</template>

<script>
import Axios from "../plugins/axios";

export default {
  name: "BookRanking",
  props: {
    visible: { type: Boolean, default: false },
    api: { type: String, default: "" }
  },
  data() {
    return {
      sites: [],
      currentSite: "",
      currentRankType: "",
      filterSelections: {},
      books: [],
      page: 1,
      hasMore: false,
      loading: false,
      errorMsg: "",
      fetchedAt: 0,
      stale: false,
      optionsLoaded: false,
      lastRequestId: 0
    };
  },
  computed: {
    currentSiteConfig() {
      return this.sites.find(s => s.siteId === this.currentSite);
    },
    currentFilters() {
      if (!this.currentSiteConfig) return [];
      return this.currentSiteConfig.filters[this.currentRankType] || [];
    }
  },
  watch: {
    visible(val) {
      if (val && !this.optionsLoaded) {
        this.loadOptions();
      }
    },
    api() {
      this.optionsLoaded = false;
      if (this.visible) {
        this.loadOptions();
      }
    }
  },
  methods: {
    loadOptions() {
      if (!this.api) return;
      Axios.get(this.api + "/getBookRankingOptions").then(
        res => {
          if (res.data.isSuccess) {
            this.sites = res.data.data;
            this.optionsLoaded = true;
            if (this.sites.length && !this.currentSite) {
              this.selectSite(this.sites[0].siteId);
            }
          }
        },
        () => {
          this.errorMsg = "获取排行榜选项失败，请检查后端连接";
        }
      );
    },
    selectSite(siteId) {
      this.currentSite = siteId;
      const config = this.currentSiteConfig;
      if (config) {
        this.currentRankType = config.defaultRankType;
        this.resetFilters();
        this.loadRankings(1);
      }
    },
    selectRankType(rankType) {
      this.currentRankType = rankType;
      this.resetFilters();
      this.loadRankings(1);
    },
    selectFilter(filterId, optionId) {
      this.$set(this.filterSelections, filterId, optionId);
      this.loadRankings(1);
    },
    resetFilters() {
      const filters = this.currentFilters;
      const selections = {};
      filters.forEach(f => {
        selections[f.id] = f.defaultId;
      });
      this.filterSelections = selections;
    },
    loadRankings(page) {
      if (!this.api || !this.currentSite || !this.currentRankType) return;
      if (page === 1) {
        this.books = [];
        this.errorMsg = "";
      }
      this.loading = true;
      this.page = page;
      const requestId = ++this.lastRequestId;

      const params = new URLSearchParams();
      params.set("site", this.currentSite);
      params.set("rankType", this.currentRankType);
      params.set("page", page.toString());
      if (this.filterSelections.gender)
        params.set("gender", this.filterSelections.gender);
      if (this.filterSelections.category)
        params.set("category", this.filterSelections.category);
      if (this.filterSelections.period)
        params.set("period", this.filterSelections.period);

      Axios.get(this.api + "/getBookRankings?" + params.toString(), {
        timeout: 30000
      }).then(
        res => {
          if (requestId !== this.lastRequestId) return;
          this.loading = false;
          if (res.data.isSuccess) {
            const data = res.data.data;
            const newItems = data.items || [];
            if (page === 1) {
              this.books = newItems;
            } else {
              const existingIds = new Set(
                this.books.map(b => b.siteBookId || b.officialUrl)
              );
              const deduped = newItems.filter(
                b => !existingIds.has(b.siteBookId || b.officialUrl)
              );
              this.books = this.books.concat(deduped);
            }
            this.hasMore = data.hasMore;
            this.fetchedAt = data.fetchedAt;
            this.stale = data.stale;
          } else {
            this.errorMsg = res.data.errorMsg || "加载失败";
          }
        },
        error => {
          if (requestId !== this.lastRequestId) return;
          this.loading = false;
          this.errorMsg = "请求失败: " + (error && error.toString());
        }
      );
    },
    loadMore() {
      if (this.hasMore && !this.loading) {
        this.loadRankings(this.page + 1);
      }
    },
    handleBookClick(book) {
      this.$emit("searchBook", book.name, book.author);
    },
    openOfficial(url) {
      window.open(url, "_blank");
    },
    getBookCover(coverUrl) {
      if (!coverUrl) return "";
      if (coverUrl.startsWith("http")) {
        return this.api + "/cover?path=" + encodeURIComponent(coverUrl);
      }
      return coverUrl;
    },
    getRankClass(rank) {
      if (rank === 1) return "top1";
      if (rank === 2) return "top2";
      if (rank === 3) return "top3";
      return "";
    },
    formatTime(timestamp) {
      if (!timestamp) return "";
      const d = new Date(timestamp);
      const pad = n => (n < 10 ? "0" + n : n);
      return pad(d.getHours()) + ":" + pad(d.getMinutes());
    },
    handleScroll() {},
    getScrollState() {
      const el = this.$refs.rankingList;
      return {
        site: this.currentSite,
        rankType: this.currentRankType,
        filters: { ...this.filterSelections },
        page: this.page,
        scrollTop: el ? el.scrollTop : 0
      };
    },
    restoreScrollState(state) {
      if (!state) return;
      if (state.site) this.currentSite = state.site;
      if (state.rankType) this.currentRankType = state.rankType;
      if (state.filters) this.filterSelections = state.filters;
      this.$nextTick(() => {
        const el = this.$refs.rankingList;
        if (el && state.scrollTop) el.scrollTop = state.scrollTop;
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.ranking-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ranking-header {
  padding: 12px 16px 8px;
  flex-shrink: 0;
}

.ranking-sites {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.ranking-site-btn {
  padding: 6px 16px;
  border: 1px solid var(--modern-line, #e4e7ed);
  border-radius: 16px;
  background: transparent;
  color: var(--modern-muted, #606266);
  font-size: 14px;
  cursor: pointer;
  transition: all 180ms ease;

  &:hover {
    border-color: var(--modern-accent, #4f6ef7);
    color: var(--modern-accent, #4f6ef7);
  }

  &.active {
    background: var(--modern-accent, #4f6ef7);
    border-color: var(--modern-accent, #4f6ef7);
    color: #fff;
  }
}

.ranking-filters {
  padding: 4px 16px 8px;
  flex-shrink: 0;
}

.filter-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.filter-btn {
  padding: 4px 12px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: var(--modern-soft, #f4f4f5);
  color: var(--modern-muted, #606266);
  font-size: 12px;
  cursor: pointer;
  transition: all 150ms ease;

  &:hover {
    background: var(--modern-line, #e4e7ed);
  }

  &.active {
    background: rgba(79, 110, 247, 0.1);
    color: var(--modern-accent, #4f6ef7);
    font-weight: 600;
  }

  &.small {
    padding: 3px 10px;
    font-size: 11px;
  }
}

.ranking-meta {
  padding: 0 16px 6px;
  font-size: 11px;
  color: var(--modern-weak, #909399);
  flex-shrink: 0;

  .ranking-stale {
    color: #e6a23c;
  }
}

.ranking-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
}

.ranking-card {
  display: grid;
  grid-template-columns: 36px 64px 1fr auto;
  gap: 12px;
  align-items: start;
  padding: 12px 0;
  border-bottom: 1px solid var(--modern-line, #f0f0f0);
  cursor: pointer;
  transition: background 150ms ease;

  &:hover {
    background: var(--modern-soft, #fafafa);
    border-radius: 8px;
    margin: 0 -8px;
    padding: 12px 8px;
  }

  &:last-child {
    border-bottom: none;
  }
}

.rank-badge {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 6px;
  background: var(--modern-soft, #f4f4f5);
  color: var(--modern-muted, #909399);
  font-size: 13px;
  font-weight: 700;

  &.top1 {
    background: linear-gradient(135deg, #ff6b35, #ff4500);
    color: #fff;
  }

  &.top2 {
    background: linear-gradient(135deg, #ffa726, #ff8f00);
    color: #fff;
  }

  &.top3 {
    background: linear-gradient(135deg, #42a5f5, #1e88e5);
    color: #fff;
  }
}

.rank-cover {
  width: 64px;
  height: 86px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;

  .el-image {
    width: 100%;
    height: 100%;
  }
}

.rank-info {
  min-width: 0;
}

.rank-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--modern-text, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-author {
  font-size: 12px;
  color: var(--modern-muted, #909399);
  margin-top: 3px;
}

.rank-meta-row {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  font-size: 11px;
}

.rank-category {
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--modern-soft, #f4f4f5);
  color: var(--modern-weak, #909399);
}

.rank-metric {
  color: var(--modern-accent, #4f6ef7);
  font-weight: 600;
}

.rank-intro,
.rank-latest {
  margin-top: 4px;
  font-size: 12px;
  color: var(--modern-weak, #909399);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-actions {
  display: flex;
  align-items: center;
}

.official-link-btn {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 1px solid var(--modern-line, #e4e7ed);
  border-radius: 6px;
  background: transparent;
  color: var(--modern-muted, #909399);
  font-size: 14px;
  cursor: pointer;

  &:hover {
    border-color: var(--modern-accent, #4f6ef7);
    color: var(--modern-accent, #4f6ef7);
  }
}

.ranking-load-more {
  padding: 16px 0;
  text-align: center;
}

.load-more-btn,
.retry-btn {
  padding: 8px 24px;
  border: 1px solid var(--modern-line, #e4e7ed);
  border-radius: 16px;
  background: transparent;
  color: var(--modern-muted, #606266);
  font-size: 13px;
  cursor: pointer;

  &:hover:not(:disabled) {
    border-color: var(--modern-accent, #4f6ef7);
    color: var(--modern-accent, #4f6ef7);
  }

  &:disabled {
    opacity: 0.5;
    cursor: default;
  }
}

.ranking-loading-more {
  padding: 12px 0;
  text-align: center;
  color: var(--modern-muted, #909399);
  font-size: 13px;
}

.ranking-error {
  padding: 60px 20px;
  text-align: center;
  color: var(--modern-muted, #909399);

  p {
    margin-bottom: 16px;
  }
}

.ranking-empty {
  padding: 60px 20px;
  text-align: center;
  color: var(--modern-weak, #c0c4cc);
  font-size: 14px;
}

/* Skeleton loading */
.ranking-skeleton {
  .skeleton-item {
    display: grid;
    grid-template-columns: 36px 64px 1fr;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid var(--modern-line, #f0f0f0);
  }
}

.skeleton-rank,
.skeleton-cover,
.skeleton-line {
  background: var(--modern-soft, #f4f4f5);
  border-radius: 4px;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

.skeleton-rank {
  width: 28px;
  height: 28px;
  border-radius: 6px;
}
.skeleton-cover {
  width: 64px;
  height: 86px;
}
.skeleton-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
}
.skeleton-line {
  height: 14px;
}
.skeleton-line.w60 {
  width: 60%;
}
.skeleton-line.w40 {
  width: 40%;
}
.skeleton-line.w80 {
  width: 80%;
}

@keyframes skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

/* Responsive: single column on narrow screens */
@media (max-width: 600px) {
  .ranking-card {
    grid-template-columns: 28px 52px 1fr auto;
    gap: 8px;
  }

  .rank-cover {
    width: 52px;
    height: 70px;
  }

  .rank-name {
    font-size: 13px;
  }
}

/* Night mode support */
.night & {
  .ranking-site-btn {
    border-color: #3a3a3a;
    color: #aaa;

    &.active {
      background: var(--modern-accent, #4f6ef7);
      border-color: var(--modern-accent, #4f6ef7);
      color: #fff;
    }
  }

  .filter-btn {
    background: #2a2a2a;
    color: #aaa;

    &.active {
      background: rgba(79, 110, 247, 0.2);
      color: #7b9aff;
    }
  }

  .ranking-card:hover {
    background: #2a2a2a;
  }

  .rank-badge {
    background: #2a2a2a;
    color: #999;
  }

  .rank-name {
    color: #eee;
  }
  .rank-category {
    background: #333;
    color: #aaa;
  }
  .skeleton-rank,
  .skeleton-cover,
  .skeleton-line {
    background: #333;
  }
}
</style>
