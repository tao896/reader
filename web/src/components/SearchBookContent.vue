<template>
  <el-dialog
    :visible.sync="show"
    :width="dialogWidth"
    :top="dialogTop"
    :fullscreen="$store.state.miniInterface"
    :class="
      isWebApp && !$store.getters.isNight ? 'status-bar-light-bg-dialog' : ''
    "
    v-if="$store.getters.isNormalPage"
    :before-close="cancel"
    @opened="opend"
  >
    <div class="custom-dialog-title" slot="title">
      <span class="el-dialog__title">
        <span class="title-input">
          <el-input
            size="mini"
            placeholder="搜索书籍内容"
            v-model="keyword"
            class="search-input"
            @keyup.enter.native="searchBookContent(-1)"
          >
            <i slot="prefix" class="el-input__icon el-icon-search"></i>
          </el-input>
          <el-switch
            v-model="caseSensitive"
            class="case-sensitive-switch"
            active-text="区分大小写"
          >
          </el-switch>
        </span>
      </span>
    </div>
    <div class="source-container table-container">
      <el-table
        ref="resultTable"
        :data="searchResultList"
        :height="dialogContentHeight"
        @row-click="clickRow"
      >
        <el-table-column property="chapterTitle" min-width="100px" label="章节">
        </el-table-column>
        <el-table-column
          property="resultText"
          min-width="250px"
          label="搜索结果"
        >
          <template slot-scope="scope">
            <span
              v-for="(part, index) in getHighlightedParts(scope.row)"
              :key="'result-part-' + index"
              :class="{ 'keyword-highlight': part.highlight }"
              >{{ part.text }}</span
            >
          </template>
        </el-table-column>
        <template slot="empty">
          {{ emptyText }}
        </template>
      </el-table>
    </div>
    <div slot="footer" class="dialog-footer">
      <el-button
        type="primary"
        size="medium"
        class="float-left"
        :disabled="loading || (searched && noMore)"
        @click="searchBookContent(searched ? lastIndex : -1)"
        >{{ searchButtonText }}</el-button
      >
      <span class="search-tip">{{ searchSummary }}</span>
      <el-button
        type="primary"
        size="medium"
        class="float-left"
        v-if="lastScrollTop > 0"
        @click="restoreScrollTop"
        >跳转上次位置</el-button
      >
      <el-button size="medium" @click="cancel">取消</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { mapGetters } from "vuex";
import Axios from "../plugins/axios";
import eventBus from "../plugins/eventBus";

export default {
  model: {
    prop: "show",
    event: "setShow"
  },
  name: "SearchBookContent",
  data() {
    return {
      lastScrollTop: 0,
      keyword: "",
      lastIndex: 0,
      searchResultList: [],
      loading: false,
      caseSensitive: true,
      searched: false,
      noMore: false
    };
  },
  computed: {
    ...mapGetters(["dialogWidth", "dialogTop", "dialogContentHeight"]),
    searchButtonText() {
      if (this.loading) {
        return "加载中";
      }
      if (!this.searched) {
        return "搜索";
      }
      return this.noMore ? "没有更多" : "加载更多";
    },
    searchSummary() {
      if (!this.keyword) {
        return "输入关键词后搜索";
      }
      if (this.loading) {
        return "正在搜索...";
      }
      if (!this.searched) {
        return "按回车开始搜索";
      }
      if (!this.searchResultList.length) {
        return this.noMore ? "未找到匹配内容" : "已搜索，暂无结果";
      }
      const chapterTip =
        this.lastIndex >= 0 ? `，已检索到第 ${this.lastIndex + 1} 章` : "";
      return `已加载 ${this.searchResultList.length} 条结果${chapterTip}`;
    },
    emptyText() {
      if (!this.keyword) {
        return "请输入搜索关键词";
      }
      if (this.loading) {
        return "搜索中...";
      }
      if (this.searched) {
        return "暂无搜索结果";
      }
      return "按回车开始搜索";
    }
  },
  props: ["show", "book", "initialKeyword"],
  watch: {
    show(isVisible) {
      if (isVisible) {
        this.applyInitialKeyword();
      }
    },
    initialKeyword() {
      if (this.show) {
        this.applyInitialKeyword();
      }
    },
    book: {
      deep: true,
      handler(newVal, oldVal) {
        if ((newVal || {}).bookUrl !== (oldVal || {}).bookUrl) {
          this.keyword = "";
          this.lastIndex = -1;
          this.searchResultList = [];
          this.searched = false;
          this.noMore = false;
        }
      }
    },
    caseSensitive() {
      if (this.searched && this.keyword) {
        this.searchBookContent(-1);
      }
    }
  },
  created() {
    window.searchBookComp = this;
  },
  methods: {
    formatTableField(row, column, cellValue) {
      switch (column.property) {
        default:
          return cellValue;
      }
    },
    applyInitialKeyword() {
      const nextKeyword = (this.initialKeyword || "")
        .replace(/^\s+/, "")
        .replace(/\s+$/, "");
      if (!nextKeyword) {
        return;
      }
      if (this.keyword === nextKeyword && this.searchResultList.length) {
        return;
      }
      this.keyword = nextKeyword;
      this.lastIndex = -1;
      this.searchResultList = [];
      this.searched = false;
      this.noMore = false;
      this.$nextTick(() => {
        this.searchBookContent(-1);
      });
    },
    getHighlightedParts(row) {
      const text = ((row && row.resultText) || "").toString();
      const query = ((row && row.query) || this.keyword || "").toString();
      if (!text || !query) {
        return [{ text, highlight: false }];
      }
      const source = this.caseSensitive ? text : text.toLowerCase();
      const target = this.caseSensitive ? query : query.toLowerCase();
      const parts = [];
      let start = 0;
      let index = source.indexOf(target, start);
      while (index >= 0) {
        if (index > start) {
          parts.push({
            text: text.slice(start, index),
            highlight: false
          });
        }
        parts.push({
          text: text.slice(index, index + query.length),
          highlight: true
        });
        start = index + query.length;
        index = source.indexOf(target, start);
      }
      if (start < text.length) {
        parts.push({
          text: text.slice(start),
          highlight: false
        });
      }
      return parts.length ? parts : [{ text, highlight: false }];
    },
    opend() {
      this.$nextTick(() => {
        this.restoreScrollTop();
      });
    },
    restoreScrollTop() {
      if (!this.$refs.resultTable || !this.$refs.resultTable.$ready) {
        this.$nextTick(() => {
          this.restoreScrollTop();
        });
        return;
      }
      try {
        this.$refs.resultTable.bodyWrapper.scrollTop = this.lastScrollTop;
      } catch (error) {
        setTimeout(() => {
          this.restoreScrollTop();
        }, 10);
      }
    },
    cancel() {
      this.$emit("setShow", false);
    },
    async searchBookContent(lastIndex) {
      if (this.loading) {
        return;
      }
      const query = this.keyword.replace(/^\s+/, "").replace(/\s+$/, "");
      if (!query) {
        this.$message.error("请输入搜索关键词");
        return;
      }
      if (lastIndex === -1) {
        this.searchResultList = [];
        this.noMore = false;
      }
      this.loading = true;
      Axios.post(
        this.api + "/searchBookContent",
        {
          url: this.book.bookUrl,
          keyword: query,
          lastIndex: lastIndex,
          caseSensitive: this.caseSensitive
        },
        { silent: true }
      ).then(
        res => {
          this.loading = false;
          this.searched = true;
          if (res.data.isSuccess) {
            this.lastIndex = res.data.data.lastIndex;
            const nextList = res.data.data.list || [];
            if (!nextList.length) {
              this.noMore = true;
            }
            if (lastIndex === -1) {
              this.searchResultList = nextList;
            } else {
              this.searchResultList = []
                .concat(this.searchResultList)
                .concat(nextList);
            }
          } else if (res.data.errorMsg === "没有更多了") {
            this.noMore = true;
          } else {
            this.$message.error(res.data.errorMsg || "加载失败");
          }
        },
        error => {
          this.loading = false;
          this.$message.error("加载失败 " + (error && error.toString()));
        }
      );
    },
    clickRow(row) {
      this.lastScrollTop = this.$refs.resultTable.bodyWrapper.scrollTop;
      eventBus.$emit("showSearchContent", row);
      this.cancel();
    }
  }
};
</script>
<style lang="stylus" scoped>
.float-left {
  float: left;
}
.title-input {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 82%;
  margin: 0 auto;
  transform: translateX(10%);
}
.search-input {
  flex: 1;
}
.case-sensitive-switch {
  white-space: nowrap;
}
.search-tip {
  float: left;
  line-height: 36px;
  margin-left: 12px;
  color: #909399;
}
.keyword-highlight {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.18);
  border-radius: 2px;
  padding: 0 1px;
}
</style>
