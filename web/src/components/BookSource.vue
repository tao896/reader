<template>
  <div class="popup-wrapper" :style="popupTheme">
    <div class="title-zone">
      <div class="title">来源({{ bookSource.length }})</div>
      <div :class="{ 'title-btn': true, loading: loadingMore }">
        <el-select
          size="mini"
          v-model="bookSourceGroup"
          class="booksource-group-select"
          filterable
          placeholder="全部分组"
        >
          <el-option
            v-for="(item, index) in $store.getters.bookSourceGroupList"
            :key="'source-group-' + index"
            :label="item.name + ' (' + item.count + ')'"
            :value="item.value"
          >
          </el-option>
        </el-select>
        <span :class="{ loading: loading }" @click="refresh">
          <i class="el-icon-loading" v-if="loading"></i>
          {{ loading ? "刷新中..." : "刷新" }}
        </span>
        <span
          :class="{ loading: loadingMore }"
          @click="searchBookSourceByEventStream"
        >
          <i class="el-icon-loading" v-if="loadingMore"></i>
          {{ loadingMore ? "加载中..." : "加载更多" }}
        </span>
      </div>
    </div>
    <div
      class="data-wrapper"
      ref="sourceList"
      :class="{ night: $store.getters.isNight, day: !$store.getters.isNight }"
    >
      <div class="source-list">
        <div
          class="source-item"
          v-for="(searchBook, index) in bookSource"
          :class="{ selected: isSelected(searchBook) }"
          :key="index"
          @click="changeBookSource(searchBook)"
          ref="source"
        >
          <div class="source-title">
            <div class="source-name">
              {{ searchBook.originName }}
            </div>
            <div class="source-time">
              {{ searchBook.time ? "⏱ " + searchBook.time + "ms" : "" }}
            </div>
          </div>
          <div class="source-latest-chapter">
            {{ searchBook.latestChapterTitle || "无最新章节" }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import jump from "../plugins/jump";
import Axios from "../plugins/axios";
const buildURL = require("axios/lib/helpers/buildURL");

export default {
  name: "BookSource",
  data() {
    return {
      index: this.$store.getters.readingBook.index,
      bookSource: [],
      bookSourceGroup: "",
      bookSourceGroupIndexMap: {},
      searchRequestToken: 0,
      loading: false,
      loadingMore: false
    };
  },
  props: ["visible"],
  computed: {
    theme() {
      return this.$store.getters.config.theme;
    },
    popupTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popup
      };
    },
    bookSourceMap() {
      return this.bookSource.reduce((c, v) => {
        c[this.getBookSourceKey(v)] = v;
        return c;
      }, {});
    },
    readingBook() {
      return this.$store.getters.readingBook || {};
    }
  },
  mounted() {},
  watch: {
    visible(isVisible) {
      if (isVisible) {
        this.getBookSource();
      } else {
        this.closeSearchEventSource();
      }
    },
    readingBook(val, oldVal) {
      const bookChanged =
        this.getBookSourceKey(val || {}) !==
        this.getBookSourceKey(oldVal || {});
      if (bookChanged) {
        this.closeSearchEventSource();
        this.loading = false;
        this.bookSource = [];
        this.bookSourceGroupIndexMap = {};
        if (this.visible && val && val.bookUrl) {
          this.getBookSource();
        }
      }
    }
  },
  beforeDestroy() {
    this.closeSearchEventSource();
  },
  methods: {
    closeSearchEventSource() {
      this.searchRequestToken += 1;
      try {
        if (
          this.searchEventSource &&
          this.searchEventSource.readyState != this.searchEventSource.CLOSED
        ) {
          this.searchEventSource.close();
        }
      } catch (error) {
        //
      }
      this.searchEventSource = null;
      this.loadingMore = false;
    },
    isActiveSearchRequest(requestToken, bookIdentity) {
      return (
        this.searchRequestToken === requestToken &&
        this.getBookSourceKey(this.$store.getters.readingBook || {}) ===
          bookIdentity
      );
    },
    getBookSourceKey(searchBook) {
      return [searchBook.origin || "", searchBook.bookUrl || ""].join("@@");
    },
    getReadingBookRequest() {
      const book = this.$store.getters.readingBook || {};
      return {
        bookUrl: book.bookUrl,
        name: book.name,
        author: book.author,
        origin: book.origin,
        originName: book.originName,
        type: book.type,
        coverUrl: book.coverUrl,
        tocUrl: book.tocUrl,
        intro: book.intro,
        kind: book.kind,
        wordCount: book.wordCount,
        latestChapterTitle: book.latestChapterTitle,
        variable: book.variable,
        originOrder: book.originOrder
      };
    },
    isSelected(searchBook) {
      const readingBook = this.$store.getters.readingBook;
      return (
        searchBook.bookUrl == readingBook.bookUrl &&
        searchBook.origin == readingBook.origin
      );
    },
    getBookSource(refresh) {
      const requestBook = this.getReadingBookRequest();
      const requestBookIdentity = this.getBookSourceKey(requestBook);
      Axios.post(
        this.api + `/getAvailableBookSource`,
        {
          url: requestBook.bookUrl,
          book: requestBook,
          refresh: refresh ? 1 : 0
        },
        {
          silent: true
        }
      ).then(
        res => {
          if (
            requestBookIdentity !==
            this.getBookSourceKey(this.$store.getters.readingBook || {})
          ) {
            return;
          }
          this.loading = false;
          if (res.data.isSuccess) {
            this.bookSource = res.data.data || [];
            if (this.bookSource.length) {
              this.jumpToActive();
            } else {
              // this.loadMoreSource();
            }
          }
        },
        error => {
          if (
            requestBookIdentity !==
            this.getBookSourceKey(this.$store.getters.readingBook || {})
          ) {
            return;
          }
          this.loading = false;
          this.$message.error(
            "获取书籍来源信息失败 " + (error && error.toString())
          );
          throw error;
        }
      );
    },
    async changeBookSource(searchBook) {
      const isInShelf = await this.$root.$children[0].isInShelf(
        this.$store.getters.readingBook,
        "加入书架之后才能切换书源, 是否加入书架?"
      );
      if (!isInShelf) {
        return;
      }
      this.closeSearchEventSource();
      Axios.post(this.api + `/setBookSource`, {
        bookUrl: this.$store.getters.readingBook.bookUrl,
        newUrl: searchBook.bookUrl,
        bookSourceUrl: searchBook.origin
      }).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.info("换源成功");
            var book = Object.assign({}, this.$store.getters.readingBook);
            var oldBookUrl = book.bookUrl;
            var newBookInfo = res.data.data || {};
            book = Object.assign(book, {
              oldBookUrl,
              name: newBookInfo.name || book.name,
              author: newBookInfo.author || book.author,
              bookUrl: newBookInfo.bookUrl || searchBook.bookUrl,
              origin: newBookInfo.origin || searchBook.origin || book.origin,
              originName:
                newBookInfo.originName ||
                searchBook.originName ||
                book.originName,
              tocUrl: newBookInfo.tocUrl || searchBook.tocUrl || book.tocUrl,
              latestChapterTitle:
                newBookInfo.latestChapterTitle ||
                searchBook.latestChapterTitle ||
                book.latestChapterTitle,
              intro: newBookInfo.intro || searchBook.intro || book.intro,
              type:
                typeof newBookInfo.type !== "undefined"
                  ? newBookInfo.type
                  : typeof searchBook.type !== "undefined"
                  ? searchBook.type
                  : book.type,
              variable:
                typeof newBookInfo.variable !== "undefined"
                  ? newBookInfo.variable
                  : typeof searchBook.variable !== "undefined"
                  ? searchBook.variable
                  : book.variable,
              coverUrl:
                newBookInfo.customCoverUrl ||
                newBookInfo.coverUrl ||
                searchBook.coverUrl ||
                book.coverUrl
            });
            this.$store.commit("updateShelfBook", book);
            delete book.oldBookUrl;
            this.$store.commit("setReadingBook", book);
            this.$emit("changeBookSource");

            // 重新加载书架
            Axios.get(this.api + `/getBookshelf`, {}).then(
              res => {
                if (res.data.isSuccess) {
                  this.$store.commit("setShelfBooks", res.data.data);
                }
              },
              () => {
                //
              }
            );
          }
        },
        error => {
          this.$message.error("换源失败 " + (error && error.toString()));
          throw error;
        }
      );
    },
    refresh() {
      if (this.loadingMore) return;
      this.loading = true;
      this.getBookSource(true);
    },
    loadMoreSource() {
      if (this.loadingMore) return;
      const requestBook = this.getReadingBookRequest();
      const requestBookIdentity = this.getBookSourceKey(requestBook);
      const requestBookSourceGroup = this.bookSourceGroup;
      this.loadingMore = true;
      Axios.post(
        this.api + `/searchBookSource`,
        {
          url: requestBook.bookUrl,
          book: requestBook,
          bookSourceGroup: requestBookSourceGroup,
          lastIndex: this.bookSourceGroupIndexMap[requestBookSourceGroup]
        },
        {
          silent: true
        }
      ).then(
        res => {
          if (
            requestBookIdentity !==
            this.getBookSourceKey(this.$store.getters.readingBook || {})
          ) {
            return;
          }
          this.loadingMore = false;
          if (res.data.isSuccess) {
            var list = res.data.data.list || [];
            this.bookSource = [].concat(
              this.bookSource,
              list.filter(v => {
                return !this.bookSourceMap[this.getBookSourceKey(v)];
              })
            );
            if (res.data.data.lastIndex != null) {
              this.bookSourceGroupIndexMap[requestBookSourceGroup] =
                res.data.data.lastIndex;
            }
          }
        },
        error => {
          if (
            requestBookIdentity !==
            this.getBookSourceKey(this.$store.getters.readingBook || {})
          ) {
            return;
          }
          this.loadingMore = false;
          this.$message.error(
            "加载更多书籍来源失败 " + (error && error.toString())
          );
          throw error;
        }
      );
    },
    searchBookSourceByEventStream() {
      if (this.loadingMore) {
        this.closeSearchEventSource();
        return;
      }
      this.closeSearchEventSource();
      const requestBook = this.getReadingBookRequest();
      const requestBookIdentity = this.getBookSourceKey(requestBook);
      const requestBookSourceGroup = this.bookSourceGroup;
      const requestToken = this.searchRequestToken + 1;
      this.searchRequestToken = requestToken;
      const params = {
        accessToken: this.$store.state.token,
        concurrentCount: this.$store.state.searchConfig.concurrentCount,
        url: requestBook.bookUrl,
        name: requestBook.name,
        author: requestBook.author,
        origin: requestBook.origin,
        originName: requestBook.originName,
        bookSourceGroup: requestBookSourceGroup,
        lastIndex: this.bookSourceGroupIndexMap[requestBookSourceGroup]
      };
      this.loadingMore = true;

      const url = buildURL(this.api + "/searchBookSourceSSE", params);

      this.searchEventSource = new EventSource(url, {
        withCredentials: true
      });
      this.searchEventSource.addEventListener("error", e => {
        if (!this.isActiveSearchRequest(requestToken, requestBookIdentity)) {
          return;
        }
        this.loadingMore = false;
        this.closeSearchEventSource();
        try {
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.errorMsg) {
              this.$message.error(result.errorMsg);
            }
          }
        } catch (error) {
          //
        }
      });
      let oldBookSourceLength = this.bookSource.length;
      this.searchEventSource.addEventListener("end", e => {
        if (!this.isActiveSearchRequest(requestToken, requestBookIdentity)) {
          return;
        }
        this.loadingMore = false;
        try {
          let hasMore = false;
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.lastIndex != null) {
              this.bookSourceGroupIndexMap[requestBookSourceGroup] =
                result.lastIndex;
            }
            hasMore = !!(result && result.hasMore);
          }
          if (this.bookSource.length === oldBookSourceLength) {
            if (hasMore) {
              this.$message.warning("本轮未找到可用书源，可继续加载");
            } else {
              this.$message.info("没有更多书源了");
            }
          }
        } catch (error) {
          //
        }
        this.closeSearchEventSource();
      });
      this.searchEventSource.addEventListener("message", e => {
        if (!this.isActiveSearchRequest(requestToken, requestBookIdentity)) {
          return;
        }
        try {
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.lastIndex != null) {
              this.bookSourceGroupIndexMap[requestBookSourceGroup] =
                result.lastIndex;
            }
            if (result.data) {
              this.bookSource = [].concat(
                this.bookSource,
                result.data.filter(v => {
                  return !this.bookSourceMap[this.getBookSourceKey(v)];
                })
              );
            }
          }
        } catch (error) {
          //
        }
      });
    },
    jumpToActive() {
      this.$nextTick(() => {
        let index = -1;
        this.bookSource.some((v, i) => {
          if (this.isSelected(v)) {
            index = i;
            return true;
          }
        });
        if (index < 0) {
          return;
        }
        let wrapper = this.$refs.sourceList;
        jump(this.$refs.source[index], {
          container: wrapper,
          duration: 0
        });
      });
    }
  }
};
</script>

<style lang="stylus" scoped>
.popup-wrapper {
  margin: -16px;
  margin-bottom: -13px;
  padding: 24px;
  padding-top: calc(24px + constant(safe-area-inset-top));
  padding-top: calc(24px + env(safe-area-inset-top));

  .title-zone {
    margin: 0 0 20px 0;
    width: 100%;
    display: flex;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: space-between;
  }

  .title {
    font-size: 18px;
    font-weight: 400;
    font-family: -apple-system, "Noto Sans", "Helvetica Neue", Helvetica, "Nimbus Sans L", Arial, "Liberation Sans", "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", "Source Han Sans SC", "Source Han Sans CN", "Microsoft YaHei", "Wenquanyi Micro Hei", "WenQuanYi Zen Hei", "ST Heiti", SimHei, "WenQuanYi Zen Hei Sharp", sans-serif;
    color: #ed4259;
    border-bottom: 1px solid #ed4259;
    width: fit-content;
  }

  .title-btn {
    font-size: 14px;
    line-height: 26px;
    color: #ed4259;
    cursor: pointer;

    .booksource-group-select {
      width: 140px;
    }
    .source-count {
      display: inline-block;
      color: #606266;
    }
    span {
      margin-left: 15px;
    }
    &.loading {
      color: #606266;
    }
  }

  .data-wrapper {
    height: 300px;
    overflow: auto;

    .source-list {
      .source-item {
        width: 100%;
        cursor: pointer;
        display: flex;
        flex-direction: column;
        max-width: 100%;
        overflow: hidden;
        padding: 8px 0;

        .source-title {
          display: flex;
          flex-direction: row;
          flex-wrap: wrap;
          justify-content: space-between;
          align-items: center;

          .source-name {
            font-size: 16px;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;

          }
          .source-time {
            float: right;
            font-size: 12px;
          }
        }

        .source-latest-chapter {
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
          color: #888;
          font-size: 14px;
          margin-top: 6px;
        }

        &.selected {
          .source-name {
            color: #EB4259;
          }
        }
      }
    }
  }

  .data-wrapper::-webkit-scrollbar {
    width: 0 !important;
  }

  .night {
    >>>.source-item {
      border-bottom: 1px solid #333;
    }
  }

  .day {
    >>>.source-item {
      border-bottom: 1px solid #eee;
    }
  }
}
</style>
