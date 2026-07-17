<template>
  <div
    class="chapter-wrapper"
    :style="bodyTheme"
    :class="{
      night: isNight,
      day: !isNight,
      'mini-interface': $store.state.miniInterface
    }"
    ref="chapterWrapperRef"
  >
    <div class="tool-bar" :style="leftBarTheme">
      <div class="tools">
        <div class="tool-icon" @click="toShelf">
          <div class="iconfont">
            &#58920;
          </div>
          <div class="icon-text">首页</div>
        </div>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="popBookShelfVisible"
          popper-class="popper-component"
        >
          <BookShelf
            ref="popBookShelf"
            class="popup"
            :visible="popBookShelfVisible"
            @changeBook="changeBook"
            @toShelf="toShelf"
          />
          <div class="tool-icon" slot="reference">
            <div class="iconfont">
              &#58892;
            </div>
            <div class="icon-text">书架</div>
          </div>
        </el-popover>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="popBookSourceVisible"
          popper-class="popper-component"
        >
          <BookSource
            ref="popBookSource"
            class="popup"
            :visible="popBookSourceVisible"
            @changeBookSource="changeBookSource()"
            @close="popBookSourceVisible = false"
          />

          <div class="tool-icon" slot="reference">
            <div class="tool-el-icon">
              <i class="el-icon-menu"></i>
            </div>
            <div class="icon-text">书源</div>
          </div>
        </el-popover>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="popCataVisible"
          popper-class="popper-component"
        >
          <PopCata
            @getContent="getContent"
            ref="popCata"
            class="popup"
            @refresh="refreshCatalog"
            :visible="popCataVisible"
            @close="popCataVisible = false"
          />

          <div class="tool-icon" slot="reference">
            <div class="iconfont">
              &#58905;
            </div>
            <div class="icon-text">目录</div>
          </div>
        </el-popover>
        <el-popover
          placement="right"
          :width="popperWidth"
          trigger="click"
          :visible-arrow="false"
          v-model="readSettingsVisible"
          popper-class="popper-component"
        >
          <ReadSettings
            class="popup"
            :visible="readSettingsVisible"
            @close="readSettingsVisible = false"
            @showClickZone="showClickZone = true"
            @readMethodChange="beforeReadMethodChange"
          />

          <div class="tool-icon" slot="reference">
            <div class="iconfont">
              &#58971;
            </div>
            <div class="icon-text">设置</div>
          </div>
        </el-popover>
        <div
          class="tool-icon"
          @click="toTop(0)"
          v-if="!$store.state.miniInterface"
        >
          <div class="iconfont">
            &#58914;
          </div>
          <div class="icon-text">顶部</div>
        </div>
        <div
          class="tool-icon"
          @click="toBottom(0)"
          v-if="!$store.state.miniInterface"
        >
          <div class="iconfont">
            &#58915;
          </div>
          <div class="icon-text">底部</div>
        </div>
      </div>
    </div>
    <div class="read-bar" :style="rightBarTheme">
      <div class="float-btn-zone">
        <div class="float-left-btn-zone">
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showBookmarkDialog"
          >
            <i class="el-icon-collection-tag"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showSearchBookContentDialog"
          >
            <i class="el-icon-search"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showReadingBookInfo"
          >
            <i class="el-icon-info"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toTop(0)"
            v-if="$store.state.miniInterface"
          >
            <i class="el-icon-top"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toBottom(0)"
            v-if="$store.state.miniInterface"
          >
            <i class="el-icon-bottom"></i>
          </div>
        </div>
        <div class="float-right-btn-zone">
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="refreshContent"
          >
            <i class="el-icon-refresh-right"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toggleAutoReading()"
            v-if="!isEpub && !isCarToon && !isAudio"
          >
            <i class="el-icon-view"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="showReadBar = !showReadBar"
            v-if="speechAvalable && !isEpub && !isCarToon && !isAudio"
          >
            <i class="el-icon-headset"></i>
          </div>
          <div
            class="float-btn"
            :style="popupAbsoluteBtnStyle"
            @click="toogleNight"
          >
            <i class="el-icon-moon" v-if="!isNight"></i>
            <i class="el-icon-sunny" v-else></i>
          </div>
        </div>
      </div>
      <div class="progress" v-if="$store.state.miniInterface && !isAudio">
        <div class="progress-bar">
          <el-slider
            v-model="currentPage"
            :min="1"
            :max="totalPages"
            :show-tooltip="false"
            @change="showPage"
            @input="progressValue = $event"
          ></el-slider>
        </div>
        <span class="progress-tip">{{ formatProgressTip() }}</span>
      </div>
      <div class="cache-content-zone" v-if="showCacheContentZone">
        <div>
          缓存章节
        </div>
        <div
          class="cache-content-btn"
          v-show="!isCachingContent"
          @click="cacheChapterContent(50)"
        >
          后面50章
        </div>
        <div
          class="cache-content-btn"
          v-show="!isCachingContent"
          @click="cacheChapterContent(100)"
        >
          后面100章
        </div>
        <div
          class="cache-content-btn"
          v-show="!isCachingContent"
          @click="cacheChapterContent(true)"
        >
          后面全部
        </div>
        <div class="caching-tip" v-show="isCachingContent">
          {{ cachingContentTip }}
        </div>
        <div
          class="caching-cancel-btn"
          v-show="isCachingContent"
          @click="cancelCaching"
        >
          <i class="el-icon-close"></i>
        </div>
      </div>
      <div class="tools">
        <div class="tool-icon progress-text" @click="showCacheContent">
          <span v-if="$store.state.miniInterface">阅读进度: </span>
          {{ readingProgress }}
        </div>
        <div
          class="tool-icon"
          @click="toLastChapter()"
          :style="$store.state.miniInterface ? { order: -1 } : {}"
        >
          <div class="iconfont">
            &#58920;
          </div>
          <span v-if="$store.state.miniInterface">上一章</span>
        </div>
        <div class="tool-icon" @click="toNextChapter()">
          <span v-if="$store.state.miniInterface">下一章</span>
          <div class="iconfont">
            &#58913;
          </div>
        </div>
      </div>
    </div>
    <div class="read-bar" :style="readBarTheme">
      <div class="reader-bar-inner">
        <div class="operate-bar">
          <div class="close-btn" @click="exitRead">
            <i class="el-icon-close"></i>
          </div>
          <div class="center">
            <span class="ctrl-btn" @click="speechPrev">上一段</span>
            <span class="play-pause-btn" @click="toggleSpeech">
              <i
                class="el-icon-video-pause"
                :style="popupAbsoluteBtnStyle"
                v-if="speechSpeaking"
              ></i>
              <i
                class="el-icon-video-play"
                :style="popupAbsoluteBtnStyle"
                v-else
              ></i>
            </span>
            <span class="ctrl-btn" @click="speechNext">下一段</span>
          </div>
          <div
            class="collapse-btn"
            @click="showSpeechConfig = !showSpeechConfig"
          >
            <i class="el-icon-bottom" v-if="showSpeechConfig"></i>
            <i class="el-icon-top" v-else></i>
          </div>
        </div>
        <div class="setting-item" v-if="showSpeechConfig">
          <div class="setting-title">语音库</div>
          <div class="setting-value">
            <div class="voice-list">
              <el-radio-group
                v-model="voiceName"
                size="small"
                class="radio-group"
              >
                <el-radio-button
                  class="radio-button"
                  :label="voice.name"
                  :key="index"
                  v-for="(voice, index) in voiceList"
                ></el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </div>
        <div class="setting-item" v-if="showSpeechConfig">
          <div class="setting-title">语音设置</div>
          <div class="setting-value">
            <div class="progress">
              <span class="progress-tip">语速</span>
              <div class="progress-bar">
                <el-slider
                  v-model="speechRate"
                  :min="0.5"
                  :max="2"
                  :step="0.1"
                  :show-tooltip="false"
                  @change="changeSpeechRate"
                ></el-slider>
              </div>
              <span class="setting-btn" @click="changeSpeechRate(1)">重置</span>
            </div>
            <div class="progress">
              <span class="progress-tip">语调</span>
              <div class="progress-bar">
                <el-slider
                  v-model="speechPitch"
                  :min="0"
                  :max="2"
                  :step="0.1"
                  :show-tooltip="false"
                  @change="changeSpeechPitch"
                ></el-slider>
              </div>
              <span class="setting-btn" @click="changeSpeechPitch(1)"
                >重置</span
              >
            </div>
            <div class="progress">
              <span class="progress-tip">定时</span>
              <div class="progress-bar">
                <el-slider
                  v-model="speechMinutes"
                  :min="0"
                  :max="180"
                  :step="1"
                  :show-tooltip="false"
                  @change="changeSpeechMinutes"
                ></el-slider>
              </div>
              <span class="setting-btn">{{ speechMinutes }}分钟</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      v-show="selectionToolbarVisible"
      ref="selectionToolbar"
      class="selection-floating-toolbar"
      :style="selectionToolbarStyle"
      @mousedown.stop.prevent
      @touchstart.stop
      @click.stop
    >
      <button
        class="selection-toolbar-btn primary"
        type="button"
        @click="runSelectionToolbarAction('dictionary')"
      >
        <i class="el-icon-reading"></i>
        <span>字典</span>
      </button>
      <button
        class="selection-toolbar-btn"
        type="button"
        @click="runSelectionToolbarAction('filter')"
      >
        <i class="el-icon-set-up"></i>
        <span>过滤</span>
      </button>
      <button
        class="selection-toolbar-btn"
        type="button"
        @click="runSelectionToolbarAction('bookmark')"
      >
        <i class="el-icon-collection-tag"></i>
        <span>书签</span>
      </button>
      <button
        class="selection-toolbar-btn"
        type="button"
        @click="runSelectionToolbarAction('search')"
      >
        <i class="el-icon-search"></i>
        <span>搜索</span>
      </button>
      <button
        class="selection-toolbar-btn icon-only"
        type="button"
        title="复制"
        @click="runSelectionToolbarAction('copy')"
      >
        <i class="el-icon-document-copy"></i>
      </button>
    </div>
    <el-dialog
      title="字典"
      :visible.sync="dictionaryVisible"
      :width="dictionaryDialogWidth"
      custom-class="dictionary-dialog"
      append-to-body
    >
      <div class="dictionary-panel" v-loading="dictionaryLoading">
        <div class="dictionary-word-row">
          <div class="dictionary-word">
            {{ dictionaryEntry.title || dictionaryEntry.query }}
          </div>
          <el-button
            size="mini"
            icon="el-icon-headset"
            @click="playDictionaryPronunciation"
            :disabled="!dictionaryEntry.text"
          >
            朗读
          </el-button>
        </div>
        <div
          class="dictionary-phonetics"
          v-if="dictionaryEntry.phonetics.length"
        >
          <span
            class="dictionary-phonetic"
            v-for="(phonetic, index) in dictionaryEntry.phonetics"
            :key="index"
            >{{ phonetic }}</span
          >
        </div>
        <div class="dictionary-tip" v-if="dictionaryEntry.isFallback">
          未找到完整词条，已显示相关字词。
        </div>
        <div class="dictionary-section" v-if="dictionaryEntry.entries.length">
          <div class="dictionary-section-title">释义</div>
          <div
            class="dictionary-meaning"
            v-for="(entry, index) in dictionaryEntry.entries"
            :key="index"
          >
            <div class="dictionary-part">
              <span v-if="entry.title">{{ entry.title }}</span>
              <span v-if="entry.pinyin">{{ entry.pinyin }}</span>
              <span v-if="entry.bopomofo">{{ entry.bopomofo }}</span>
            </div>
            <ol>
              <li
                v-for="(definition, definitionIndex) in entry.definitions"
                :key="definitionIndex"
              >
                <div>
                  <span class="dictionary-type" v-if="definition.type">
                    {{ definition.type }}
                  </span>
                  {{ definition.def }}
                </div>
                <div class="dictionary-example" v-if="definition.examples">
                  {{ definition.examples }}
                </div>
                <div class="dictionary-example" v-if="definition.quotes">
                  {{ definition.quotes }}
                </div>
                <div class="dictionary-example" v-if="definition.synonyms">
                  近义：{{ definition.synonyms }}
                </div>
                <div class="dictionary-example" v-if="definition.antonyms">
                  反义：{{ definition.antonyms }}
                </div>
              </li>
            </ol>
          </div>
        </div>
        <div class="dictionary-error" v-if="dictionaryError">
          {{ dictionaryError }}
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button size="mini" @click="dictionaryVisible = false">
          关闭
        </el-button>
      </div>
    </el-dialog>
    <div
      class="chapter"
      ref="content"
      :class="chapterClass"
      :style="chapterTheme"
    >
      <div
        class="click-zone"
        v-if="showClickZone"
        :style="!isSlideRead ? { position: 'fixed' } : {}"
      >
        <div :style="showPrevPageStyle"><span>点击前一页</span></div>
        <div :style="showMenuZoneStyle"><span>点击显示菜单</span></div>
        <div :style="showNextPageStyle"><span>点击后一页</span></div>
        <div class="close-btn" @click="showClickZone = false">关闭</div>
      </div>
      <div class="top-bar" ref="top">
        {{ $store.state.miniInterface ? title : "" }}
      </div>
      <div
        class="content"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
        @mouseup="handleSelectionEnd"
        @click="handlerClick"
      >
        <div class="content-inner" v-if="show">
          <Content
            class="book-content"
            :title="title"
            :content="content"
            :showContent="show"
            :error="error"
            :style="contentStyle"
            :showChapterList="showChapterList"
            :isScrollRead="isScrollRead"
            ref="bookContentRef"
            @prevChapter="toLastChapter"
            @nextChapter="toNextChapter"
            @updateProgress="saveReadingPosition"
            @iframeLoad="$emit('iframeLoad')"
            @contentChange="computePages()"
            @epubClick="eventHandler"
            @epubBeforeLocationChange="epubBeforeLocationChangeHandler"
            @epubLocationChange="epubLocationChangeHandler"
            @epubClickHash="epubClickHash"
            @epubKeydown="keydownHandler($event, true)"
            @epubSelection="handleEpubSelection"
          />
        </div>
      </div>
      <div class="bottom-bar" ref="bottom">
        <span v-if="isSlideRead">{{
          `第${currentPage}/${totalPages}页 ${readingProgress}`
        }}</span>
        <span v-if="isSlideRead">{{ timeStr }}</span>
        <span
          class="bottom-btn"
          v-if="show && !isSlideRead && !error && !isScrollRead"
          @click="toNextChapter()"
          >加载下一章</span
        >
      </div>
    </div>
  </div>
</template>

<script>
import PopCata from "../components/PopCatalog.vue";
import ReadSettings from "../components/ReadSettings.vue";
import BookSource from "../components/BookSource.vue";
import BookShelf from "../components/BookShelf.vue";
import Content from "../components/Content.vue";
import Axios from "../plugins/axios";
import jump from "../plugins/jump";
import Animate from "../plugins/animate";
import { setCache, getCache } from "../plugins/cache";
import { simplized, traditionalized } from "../plugins/chinese";
import {
  LimitResquest,
  networkFirstRequest,
  editDistance
} from "../plugins/helper";
import { defaultReplaceRule, defaultBookmark } from "../plugins/config.js";
import { applyReplaceRulesToText } from "../plugins/replace-rule";
import eventBus from "../plugins/eventBus";
// eslint-disable-next-line no-useless-escape
const symboRegex = /[\u2000-\u206F\u2E00-\u2E7F\\'!"#$%&\(\)*+,-\./:;<=>?@\[\]^_`{\|}~，。？《》；：、«]/g;
const readingPositionTypes = Object.freeze({
  text: "textOffset",
  ratio: "chapterRatio",
  audio: "audioSeconds"
});
const readingPositionTypeList = Object.keys(readingPositionTypes).map(
  key => readingPositionTypes[key]
);
const readingPositionRatioScale = 1000000;
const readingPositionDebounce = 1000;
const readingPositionMaxWait = 10000;

export default {
  components: {
    PopCata,
    BookSource,
    BookShelf,
    Content,
    ReadSettings
  },
  mounted() {
    window.readerPage = this;
    this.speechAvalable =
      window.speechSynthesis && window.speechSynthesis.getVoices;
    if (this.speechAvalable) {
      this.fetchVoiceList();
      if (window.speechSynthesis.onvoiceschanged !== undefined) {
        window.speechSynthesis.onvoiceschanged = this.fetchVoiceList;
      }
    }
    window.addEventListener("pagehide", this.pageHideHandler);
    window.addEventListener("online", this.retryPendingReadingProgress);
    document.addEventListener(
      "visibilitychange",
      this.readingVisibilityChangeHandler
    );
    eventBus.$on("showSearchContent", data => {
      if (this._inactive) {
        return;
      }
      if (this.chapterIndex === data.chapterIndex) {
        this.showMatchKeyword(data);
        return;
      }
      if (this.isScrollRead) {
        this.scrollStartChapterIndex = data.chapterIndex;
        this.computeShowChapterList().then(() => {
          this.showMatchKeyword(data);
        });
        return;
      }
      this.$once("showContent", () => {
        this.showMatchKeyword(data);
      });
      this.getContent(data.chapterIndex);
    });
    eventBus.$on("showBookmark", bookmark => {
      if (this._inactive) {
        return;
      }
      // console.log(this.chapterIndex, bookmark);
      if (this.chapterIndex === bookmark.chapterIndex) {
        this.showBookmark(bookmark);
        return;
      }
      if (this.isScrollRead) {
        this.scrollStartChapterIndex = bookmark.chapterIndex;
        this.computeShowChapterList().then(() => {
          this.showBookmark(bookmark);
        });
        return;
      }
      this.$once("showContent", () => {
        this.showBookmark(bookmark);
      });
      this.getContent(bookmark.chapterIndex);
    });
  },
  activated() {
    this.init();
    this.consumeReaderPanelQuery();
    window.addEventListener("keydown", this.keydownHandler);
    if (this.title) {
      document.title =
        this.$store.getters.readingBook.name + " - " + this.title;
    } else {
      document.title = this.$store.getters.readingBook.name;
    }
    this.formatTime();
    this.timer = setInterval(() => {
      this.formatTime();
    }, 5000);
    this.unwatchFn = this.$store.watch(
      state => state.config,
      () => {
        this.$nextTick(() => {
          this.computePages(() => {
            if (this.currentPage > this.totalPages) {
              this.showPage(this.totalPages, 0);
            }
          });
        });
      },
      {
        deep: true
      }
    );
    window.addEventListener("scroll", this.scrollHandler);
    try {
      this.releaseWakeLockFn = this.wakeLock();
    } catch (e) {
      //
    }
    this.$Lazyload.$on("loaded", this.lazyloadHandler);
    document.addEventListener("mousedown", this.selectionOutsideHandler, true);
    document.addEventListener("touchstart", this.selectionOutsideHandler, true);
    window.addEventListener("resize", this.hideSelectionToolbar);
  },
  beforeRouteLeave(to, from, next) {
    this.prepareToLeaveReader();
    next();
  },
  deactivated() {
    this.prepareToLeaveReader();
    this.readingProgressInitId++;
    if (this.scrollTimer) {
      clearTimeout(this.scrollTimer);
      this.scrollTimer = null;
    }
    this.visualRestoreUntil = 0;
    this.lastReadingBook = this.$store.getters.readingBook;
    if (this.selectionCheckTimer) {
      clearTimeout(this.selectionCheckTimer);
      this.selectionCheckTimer = null;
    }
    this.hideSelectionToolbar();
    this.timer && clearInterval(this.timer);
    window.removeEventListener("keydown", this.keydownHandler);
    window.removeEventListener("scroll", this.scrollHandler);
    document.removeEventListener(
      "mousedown",
      this.selectionOutsideHandler,
      true
    );
    document.removeEventListener(
      "touchstart",
      this.selectionOutsideHandler,
      true
    );
    window.removeEventListener("resize", this.hideSelectionToolbar);
    this.unwatchFn && this.unwatchFn();
    this.releaseWakeLockFn && this.releaseWakeLockFn();
    this.$Lazyload.$off("loaded", this.lazyloadHandler);
  },
  beforeDestroy() {
    window.removeEventListener("pagehide", this.pageHideHandler);
    window.removeEventListener("online", this.retryPendingReadingProgress);
    document.removeEventListener(
      "visibilitychange",
      this.readingVisibilityChangeHandler
    );
    this.clearReadingProgressTimers();
  },
  watch: {
    chapterName(to) {
      this.title = to;
    },
    content() {
      this.contentStyle = {};
      this.transformX = 0;
      this.currentPage = 1;
      this.$nextTick(() => {
        this.computePages();
        this.saveReadingPosition();
      });
      if (this.isEpub) {
        this.$once("iframeLoad", () => {
          this.computePages();
        });
      }
    },
    readSettingsVisible(visible) {
      if (!visible) {
        //
      }
    },
    title(title) {
      if (title) {
        document.title = this.$store.getters.readingBook.name + " - " + title;
      } else {
        document.title = this.$store.getters.readingBook.name;
      }
    },
    isSlideRead(val) {
      if (!val) {
        this.contentStyle = {};
        this.transformX = 0;
      }
      this.$nextTick(() => {
        this.computePages(() => {
          if (this.currentParagraph) {
            this.showParagraph(this.currentParagraph, true);
          } else {
            this.showPage(this.currentPage, 0);
          }
        });
      });
    },
    isScrollRead(val) {
      if (val) {
        this.scrollStartChapterIndex = this.chapterIndex;
        this.computeShowChapterList();
      }
    },
    windowSize() {
      this.$nextTick(() => {
        this.computePages(() => {
          this.showPage(this.currentPage, 0);
        });
      });
    },
    loginAuth(val) {
      if (val) {
        this.init(true);
      }
    },
    showReadBar(val) {
      if (val) {
        this.showToolBar = false;
      }
    },
    currentPage(val, oldVal) {
      const readingBook = this.$store.getters.readingBook || {};
      const catalog = Array.isArray(readingBook.catalog)
        ? readingBook.catalog
        : [];
      // 还剩两页的时候，预读下一章节
      if (val !== oldVal && val >= this.totalPages - 2) {
        if ((readingBook.index || 0) < catalog.length - 1) {
          if (!this.isScrollRead) {
            if (!this.preCaching) {
              this.preCaching = true;
              this.getBookContent(
                (readingBook.index || 0) + 1,
                {
                  timeout: 30000,
                  silent: true
                },
                false,
                true
              ).then(() => {
                this.preCaching = false;
              });
            }
          }
        }
      }
    },
    filterRules() {
      if (this.isScrollRead) {
        //
        this.computeShowChapterList();
      } else {
        this.content = this.filterContent(this.content);
      }
    },
    chineseFont() {
      this.title = this.filterContent(this.title);
      this.content = this.filterContent(this.content);
      this.computeShowChapterList();
    }
  },
  data() {
    return {
      title: "",
      content: "",
      error: false,
      popCataVisible: false,
      readSettingsVisible: false,
      popBookSourceVisible: false,
      popBookShelfVisible: false,
      showToolBar: true,
      book: null,
      show: false,
      contentStyle: {},
      currentPage: 1,
      totalPages: 1,
      transformX: 0,
      transforming: false,
      showLastPage: false,
      showClickZone: false,
      timeStr: "",
      progressValue: 1,

      speechAvalable: false,
      showReadBar: false,
      voiceList: [],
      speechSpeaking: false,
      showSpeechConfig: true,

      currentParagraph: null,
      lastSelection: false,
      selectionActionPrompting: false,
      selectionToolbarVisible: false,
      selectionToolbarText: "",
      selectionToolbarStyle: {},
      selectionToolbarShownAt: 0,
      selectionCheckTimer: null,
      showTextFilterPrompting: false,
      showAddBookmarking: false,
      dictionaryVisible: false,
      dictionaryLoading: false,
      dictionaryError: "",
      dictionaryLookupId: 0,
      dictionaryCache: {},
      dictionaryEntry: {
        text: "",
        query: "",
        title: "",
        isFallback: false,
        phonetics: [],
        entries: []
      },

      startSavePosition: false,
      restoreReadingProgress: null,
      pendingReadingProgress: null,
      readingProgressQueue: {},
      readingProgressSyncTimer: null,
      readingProgressMaxTimer: null,
      readingProgressRequest: null,
      readingProgressCancelSource: null,
      readingProgressInFlight: null,
      readingProgressInitId: 0,
      visualRestoreUntil: 0,

      showCacheContentZone: false,
      isCachingContent: false,
      cachingContentTip: "",

      autoReading: false,
      showChapterList: [],

      scrollStartChapterIndex: 0,
      showNextChapterSize: 1,
      showPrevChapterSize: 0,

      speechMinutes: 0,
      speechEndTime: 0
    };
  },
  computed: {
    readingBook() {
      return this.$store.getters.readingBook || {};
    },
    catalog() {
      return (this.$store.getters.readingBook || {}).catalog || [];
    },
    chapterIndex() {
      return ((this.$store.getters.readingBook || {}).index || 0) | 0;
    },
    windowSize() {
      return this.$store.state.windowSize;
    },
    config() {
      return this.$store.getters.config;
    },
    theme() {
      return this.config.theme;
    },
    animateMSTime() {
      return this.config.animateMSTime;
    },
    isNight() {
      return this.$store.getters.isNight;
    },
    bodyTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.body
      };
    },
    isSlideRead() {
      return this.autoReading ||
        this.showReadBar ||
        this.isEpub ||
        this.isCarToon ||
        this.isAudio
        ? false
        : this.$store.getters.isSlideRead;
    },
    isScrollRead() {
      return (
        !this.isEpub &&
        !this.isAudio &&
        !this.isSlideRead &&
        (this.config.readMethod === "上下滚动" ||
          this.config.readMethod === "上下滚动2")
      );
    },
    chapterClass() {
      return this.isSlideRead
        ? "slide-reader"
        : this.isEpub
        ? "epub"
        : this.isCarToon
        ? "cartoon"
        : this.isAudio
        ? "audio"
        : "";
    },
    chapterTheme() {
      let readingStyle = this.showReadBar
        ? { paddingBottom: (this.showSpeechConfig ? 280 : 80) + "px" }
        : {};
      if (typeof this.$store.getters.currentThemeConfig.content === "string") {
        return {
          ...readingStyle,
          background: this.$store.getters.currentThemeConfig.content,
          width: this.readWidth
        };
      } else {
        return {
          ...readingStyle,
          ...this.$store.getters.currentThemeConfig.content,
          width: this.readWidth
        };
      }
    },
    leftBarTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popup,
        marginLeft: this.$store.state.miniInterface
          ? 0
          : -(this.readWidthConfig / 2 + 68) + "px",
        display:
          this.$store.state.miniInterface && !this.showToolBar
            ? "none"
            : "block"
      };
    },
    rightBarTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popupPure,
        marginRight: this.$store.state.miniInterface
          ? 0
          : -(this.readWidthConfig / 2 + 52) + "px",
        display:
          this.$store.state.miniInterface && !this.showToolBar
            ? "none"
            : "block"
      };
    },
    readBarTheme() {
      return {
        background: this.$store.getters.currentThemeConfig.popupPure,
        marginRight: this.$store.state.miniInterface
          ? 0
          : -(this.readWidthConfig / 2) + "px",
        zIndex: 200,
        display: this.speechAvalable && this.showReadBar ? "block" : "none",
        width: this.$store.state.miniInterface ? "100vw" : "500px"
      };
    },
    readWidth() {
      if (!this.$store.state.miniInterface) {
        return this.readWidthConfig - 130 + "px";
      } else {
        return this.windowSize.width + "px";
      }
    },
    readWidthConfig() {
      var width = this.$store.getters.config.readWidth;
      while (width > this.$store.state.windowSize.width - 140) {
        width -= 20;
      }
      return width;
    },
    popperWidth() {
      if (!this.$store.state.miniInterface) {
        return this.readWidthConfig - 33;
      } else {
        return this.windowSize.width - 33;
      }
    },
    dictionaryDialogWidth() {
      return this.$store.state.miniInterface ? "92vw" : "520px";
    },
    readingProgress() {
      if (this.catalog && this.catalog.length) {
        var chapterProgress =
          this.totalPages > 1 ? (this.currentPage - 1) / this.totalPages : 0;
        var progress =
          ((this.chapterIndex + chapterProgress) / this.catalog.length) * 100;
        return progress.toFixed(1) + "%";
      } else {
        return "";
      }
    },
    showPrevPageStyle() {
      if (this.isSlideRead) {
        // 左半部
        return {
          left: 0,
          top: 0,
          bottom: 0,
          right: this.windowSize.width / 2 + "px",
          background: "#43987324",
          paddingRight: this.windowSize.width * 0.2 + "px"
        };
      } else {
        // 上半部
        return {
          left: 0,
          top: 0,
          right: 0,
          bottom: this.windowSize.height / 2 + "px",
          background: "#43987324"
        };
      }
    },
    showMenuZoneStyle() {
      return {
        top: this.windowSize.height * 0.3 + "px",
        bottom: this.windowSize.height * 0.3 + "px",
        left: this.windowSize.width * 0.3 + "px",
        right: this.windowSize.width * 0.3 + "px",
        background: "#636060",
        zIndex: 10
      };
    },
    showNextPageStyle() {
      if (this.isSlideRead) {
        // 右半部
        return {
          right: 0,
          top: 0,
          bottom: 0,
          left: this.windowSize.width / 2 + "px",
          background: "#6b1a7324",
          paddingLeft: this.windowSize.width * 0.2 + "px"
        };
      } else {
        // 下半部
        return {
          left: 0,
          bottom: 0,
          right: 0,
          top: this.windowSize.height / 2 + "px",
          background: "#6b1a7324"
        };
      }
    },
    loginAuth() {
      return this.$store.state.loginAuth;
    },
    filterRules() {
      return this.$store.state.filterRules;
    },
    themeBtnStyle() {
      // if (this.$store.getters.isNight) {
      //   return {
      //     background: "#f7f7f7"
      //   };
      // } else {
      //   return {
      //     background: "#222"
      //   };
      // }
      return {
        background: this.$store.getters.currentThemeConfig.popupPure
      };
    },
    popupAbsoluteBtnStyle() {
      return {
        background: this.$store.getters.currentThemeConfig.popupPure
      };
    },
    voiceName: {
      get() {
        return this.$store.state.speechVoiceConfig.voiceName;
      },
      set(val) {
        if (val !== this.$store.state.speechVoiceConfig.voiceName) {
          if (this.speechSpeaking) {
            this.restartSpeech();
          }
        }
        this.$store.commit("setSpeechVoiceConfig", {
          ...this.$store.state.speechVoiceConfig,
          voiceName: val
        });
      }
    },
    speechRate: {
      get() {
        return this.$store.state.speechVoiceConfig.speechRate;
      },
      set(val) {
        if (val !== this.$store.state.speechVoiceConfig.speechRate) {
          if (this.speechSpeaking) {
            this.restartSpeech();
          }
        }
        this.$store.commit("setSpeechVoiceConfig", {
          ...this.$store.state.speechVoiceConfig,
          speechRate: val
        });
      }
    },
    speechPitch: {
      get() {
        return this.$store.state.speechVoiceConfig.speechPitch;
      },
      set(val) {
        if (val !== this.$store.state.speechVoiceConfig.speechPitch) {
          if (this.speechSpeaking) {
            this.restartSpeech();
          }
        }
        this.$store.commit("setSpeechVoiceConfig", {
          ...this.$store.state.speechVoiceConfig,
          speechPitch: val
        });
      }
    },
    isCarToon() {
      return (
        !this.error &&
        !this.isEpub &&
        !this.isCbz &&
        !this.isEpubBook &&
        (this.content || "").indexOf("<img") >= 0
      );
    },
    isAudio() {
      return !this.error && this.$store.getters.readingBook.type === 1;
    },
    isEpub() {
      return !this.error && this.isEpubIframeContent(this.content);
    },
    isEpubBook() {
      return ((this.$store.getters.readingBook || {}).bookUrl || "")
        .toLowerCase()
        .endsWith(".epub");
    },
    isCbz() {
      return (
        !this.error &&
        this.$store.getters.readingBook.bookUrl.toLowerCase().endsWith(".cbz")
      );
    },
    scrollOffset() {
      // 两行 + 两个段间距
      return (
        this.$store.getters.config.fontSize *
          this.$store.getters.config.lineHeight *
          2 +
        this.$store.getters.config.fontSize *
          this.$store.getters.config.paragraphSpace *
          2
      );
    },
    formatedTitle() {
      return this.formatChinese(this.title);
    },
    chineseFont() {
      return this.config.chineseFont;
    }
  },
  methods: {
    prepareToLeaveReader() {
      if (!this.startSavePosition) {
        return null;
      }
      const progress = this.saveReadingPosition({ immediate: true });
      this.startSavePosition = false;
      return progress;
    },
    isEpubIframeContent(content) {
      return /^\/epub\/.*\.x?html?([?#].*)?$/i.test((content || "").trim());
    },
    isValidReadingPositionType(positionType) {
      return readingPositionTypeList.indexOf(positionType) >= 0;
    },
    getReadingProgressCacheKey(book) {
      book = book || this.$store.getters.readingBook || {};
      return (
        this.getReadingProgressCachePrefix(book.userName) +
        encodeURIComponent(book.bookUrl || "")
      );
    },
    getReadingProgressCachePrefix(userName) {
      return (
        "bookChapterProgress@" +
        encodeURIComponent(
          userName || this.$store.getters.currentUserName || "default"
        ) +
        "@"
      );
    },
    getLegacyReadingProgressCacheKey(book) {
      book = book || this.$store.getters.readingBook || {};
      return "bookChapterProgress@" + book.name + "_" + book.author;
    },
    normalizeReadingProgress(progress, book) {
      if (!progress || typeof progress !== "object") {
        return null;
      }
      book = book || this.$store.getters.readingBook || {};
      const chapterIndex = Number(
        typeof progress.chapterIndex === "undefined"
          ? typeof progress.durChapterIndex === "undefined"
            ? progress.index
            : progress.durChapterIndex
          : progress.chapterIndex
      );
      const position = Number(
        typeof progress.position === "undefined"
          ? progress.durChapterPos
          : progress.position
      );
      if (
        !Number.isFinite(chapterIndex) ||
        chapterIndex < 0 ||
        !Number.isFinite(position) ||
        position < 0
      ) {
        return null;
      }
      const rawPositionType =
        progress.positionType || progress.durChapterPositionType || null;
      if (
        rawPositionType &&
        !this.isValidReadingPositionType(rawPositionType)
      ) {
        return null;
      }
      if (
        rawPositionType === readingPositionTypes.ratio &&
        position > readingPositionRatioScale
      ) {
        return null;
      }
      if (progress.pending === true && !rawPositionType && !progress.legacy) {
        return null;
      }
      return {
        bookUrl: progress.bookUrl || progress.url || book.bookUrl || "",
        userName:
          progress.userName ||
          book.userName ||
          this.$store.getters.currentUserName ||
          "default",
        chapterIndex: Math.floor(chapterIndex),
        chapterTitle: progress.chapterTitle || progress.durChapterTitle || "",
        position: Math.floor(position),
        positionType: this.isValidReadingPositionType(rawPositionType)
          ? rawPositionType
          : null,
        updatedAt: Number(
          typeof progress.updatedAt === "undefined"
            ? progress.durChapterTime || 0
            : progress.updatedAt || 0
        ),
        pending: progress.pending === true,
        legacy: progress.legacy === true,
        confirmed: progress.confirmed === true
      };
    },
    getServerReadingProgress(book) {
      book = book || this.$store.getters.readingBook || {};
      if (
        !book.bookUrl ||
        typeof book.durChapterIndex === "undefined" ||
        book.durChapterIndex === null
      ) {
        return null;
      }
      const progress = this.normalizeReadingProgress(book, book);
      if (progress) {
        progress.confirmed = true;
      }
      return progress;
    },
    getCachedReadingProgress(book) {
      book = book || this.$store.getters.readingBook || {};
      const cached = this.normalizeReadingProgress(
        getCache(this.getReadingProgressCacheKey(book)),
        book
      );
      if (cached && cached.bookUrl === book.bookUrl) {
        return cached;
      }
      const legacyPosition = Number(
        getCache(this.getLegacyReadingProgressCacheKey(book))
      );
      if (Number.isFinite(legacyPosition) && legacyPosition > 0) {
        return {
          bookUrl: book.bookUrl,
          userName: this.$store.getters.currentUserName || "default",
          chapterIndex: Math.max(
            0,
            Number(
              typeof book.durChapterIndex === "undefined"
                ? book.index || 0
                : book.durChapterIndex
            ) | 0
          ),
          chapterTitle: book.durChapterTitle || "",
          position: Math.floor(legacyPosition),
          positionType: null,
          updatedAt: 0,
          pending: true,
          legacy: true,
          confirmed: false
        };
      }
      return null;
    },
    getPendingReadingProgressList() {
      if (!window.localStorage) {
        return [];
      }
      const userName = this.$store.getters.currentUserName || "default";
      const prefix = this.getReadingProgressCachePrefix(userName);
      const progressMap = {};
      for (let i = 0; i < window.localStorage.length; i++) {
        const key = window.localStorage.key(i);
        if (!key || key.indexOf(prefix) !== 0) {
          continue;
        }
        const progress = this.normalizeReadingProgress(getCache(key), {
          userName
        });
        if (
          progress &&
          progress.userName === userName &&
          progress.pending &&
          !progress.legacy &&
          progress.bookUrl
        ) {
          progressMap[progress.bookUrl] = progress;
        }
      }
      return Object.keys(progressMap).map(bookUrl => progressMap[bookUrl]);
    },
    selectReadingProgress(serverProgress, localProgress) {
      if (localProgress && localProgress.pending) {
        if (
          !localProgress.legacy ||
          !serverProgress ||
          (serverProgress.chapterIndex === localProgress.chapterIndex &&
            serverProgress.position === 0 &&
            !serverProgress.positionType)
        ) {
          return localProgress;
        }
      }
      return serverProgress || localProgress || null;
    },
    setCachedReadingProgress(progress, pending, updatedAt) {
      if (!progress || !progress.bookUrl) {
        return;
      }
      setCache(this.getReadingProgressCacheKey(progress), {
        bookUrl: progress.bookUrl,
        userName:
          progress.userName || this.$store.getters.currentUserName || "default",
        chapterIndex: progress.chapterIndex,
        chapterTitle: progress.chapterTitle || "",
        position: progress.position,
        positionType: progress.positionType,
        updatedAt:
          typeof updatedAt === "undefined"
            ? progress.updatedAt || Date.now()
            : updatedAt,
        pending: pending === true,
        confirmed: pending === true ? false : progress.confirmed === true
      });
    },
    isReadingProgressEqual(left, right) {
      return !!(
        left &&
        right &&
        left.bookUrl === right.bookUrl &&
        (left.userName || this.$store.getters.currentUserName) ===
          (right.userName || this.$store.getters.currentUserName) &&
        left.chapterIndex === right.chapterIndex &&
        left.position === right.position &&
        left.positionType === right.positionType
      );
    },
    isBookInShelf(bookUrl) {
      return this.$store.state.shelfBooks.some(
        book => book.bookUrl === bookUrl
      );
    },
    async loadLatestReadingProgress(bookUrl) {
      let book = this.$store.getters.readingBook || {};
      const progressUserName = this.$store.getters.currentUserName;
      if (!bookUrl || book.bookUrl !== bookUrl) {
        return null;
      }
      let shelfBook = this.$store.state.shelfBooks.find(
        item => item.bookUrl === bookUrl
      );
      try {
        const response = await Axios.get(this.api + "/getShelfBook", {
          params: { url: bookUrl },
          silent: true,
          timeout: 3000
        });
        if (
          response &&
          response.data &&
          response.data.isSuccess &&
          response.data.data &&
          progressUserName === this.$store.getters.currentUserName
        ) {
          shelfBook = response.data.data;
          if (this.isBookInShelf(bookUrl)) {
            this.$store.commit("updateShelfBook", shelfBook);
          } else {
            this.$store.commit(
              "setShelfBooks",
              this.$store.state.shelfBooks.concat([shelfBook])
            );
          }
        }
      } catch (error) {
        // Reading can continue with the local shelf and pending progress.
      }
      if ((this.$store.getters.readingBook || {}).bookUrl !== bookUrl) {
        return null;
      }
      if (progressUserName !== this.$store.getters.currentUserName) {
        return null;
      }
      book = {
        ...this.$store.getters.readingBook,
        ...(shelfBook || {})
      };
      const serverProgress = shelfBook
        ? this.getServerReadingProgress(book)
        : null;
      const localProgress = this.getCachedReadingProgress(book);
      if (
        localProgress &&
        !localProgress.pending &&
        !localProgress.legacy &&
        !localProgress.confirmed &&
        this.isBookInShelf(bookUrl)
      ) {
        localProgress.pending = true;
        this.setCachedReadingProgress(
          localProgress,
          true,
          localProgress.updatedAt
        );
      }
      const selectedProgress =
        localProgress && localProgress.pending
          ? this.selectReadingProgress(serverProgress, localProgress)
          : serverProgress && localProgress
          ? serverProgress.updatedAt >= localProgress.updatedAt
            ? serverProgress
            : localProgress
          : serverProgress || localProgress;
      this.restoreReadingProgress = selectedProgress;
      if (selectedProgress) {
        this.$store.commit("setReadingBook", {
          ...this.$store.getters.readingBook,
          index: selectedProgress.chapterIndex
        });
        if (
          !selectedProgress.pending &&
          !selectedProgress.legacy &&
          selectedProgress.confirmed
        ) {
          this.$store.commit("setReadingProgress", {
            ...selectedProgress,
            setCurrentIndex: true
          });
          this.setCachedReadingProgress(
            selectedProgress,
            false,
            selectedProgress.updatedAt
          );
        }
      }
      return selectedProgress;
    },
    consumeReaderPanelQuery() {
      const panel = this.$route.query && this.$route.query.panel;
      if (panel !== "catalog" && panel !== "source") {
        return;
      }
      this.$nextTick(() => {
        if (panel === "catalog") {
          this.popCataVisible = true;
        } else if (panel === "source") {
          this.popBookSourceVisible = true;
        }
      });
      const query = { ...this.$route.query };
      delete query.panel;
      this.$router
        .replace({
          path: this.$route.path,
          query
        })
        .catch(() => {});
    },
    async init(refresh, refreshCatalog) {
      if (this.$store.getters.readingBook) {
        const initialBook = this.$store.getters.readingBook;
        if (!initialBook.bookUrl) {
          this.$message.error("请在书架选择书籍");
          return;
        }
        const hasCatalog = book =>
          !!(book && Array.isArray(book.catalog) && book.catalog.length);
        if (
          !hasCatalog(initialBook) &&
          this.lastReadingBook &&
          this.lastReadingBook.bookUrl === initialBook.bookUrl &&
          hasCatalog(this.lastReadingBook)
        ) {
          // The shelf writes a compact readingBook without catalog. Reuse the
          // retained catalog when reopening the same book so progress and page
          // navigation do not disappear while the existing content is reused.
          this.$store.commit("setReadingBook", {
            ...this.lastReadingBook,
            ...initialBook,
            catalog: this.lastReadingBook.catalog
          });
        }
        const initId = ++this.readingProgressInitId;
        const initialIndex = initialBook.index || 0;
        const activeBook = this.$store.getters.readingBook;
        let shouldReload =
          refresh ||
          !hasCatalog(activeBook) ||
          !this.lastReadingBook ||
          this.lastReadingBook.bookUrl !== activeBook.bookUrl;
        if (shouldReload) {
          this.title = "";
          this.show = false;
          this.loading = this.$loading({
            target: this.$refs.content,
            lock: true,
            text: "正在获取内容",
            spinner: "el-icon-loading",
            background: "rgba(0,0,0,0)"
          });
        }
        await this.loadLatestReadingProgress(initialBook.bookUrl);
        this.retryPendingReadingProgress();
        if (
          initId !== this.readingProgressInitId ||
          initialBook.bookUrl !==
            (this.$store.getters.readingBook || {}).bookUrl
        ) {
          return;
        }
        const latestBook = this.$store.getters.readingBook;
        shouldReload =
          shouldReload ||
          !hasCatalog(latestBook) ||
          initialIndex !== (latestBook.index || 0);
        if (shouldReload) {
          if (!this.loading || !this.loading.visible) {
            this.loading = this.$loading({
              target: this.$refs.content,
              lock: true,
              text: "正在获取内容",
              spinner: "el-icon-loading",
              background: "rgba(0,0,0,0)"
            });
          }
          this.lastReadingBook = this.$store.getters.readingBook;
          // 跳转记住的位置
          this.autoShowPosition();
          this.loadCatalog(!!refreshCatalog, true);
        } else {
          this.startSavePosition = false;
          if (this.isScrollRead) {
            this.scrollStartChapterIndex = this.chapterIndex;
            this.showPrevChapterSize = 0;
            this.computeShowChapterList();
          } else {
            this.autoShowPosition(true);
          }
        }
      } else {
        this.$message.error("请在书架选择书籍");
      }
    },
    changeBook(book) {
      this.saveReadingPosition({ immediate: true });
      this.$message.info("换书成功");
      this.popBookShelfVisible = false;
      this.show = false;
      this.startSavePosition = false;
      this.restoreReadingProgress = null;
      this.$store.commit("setReadingBook", book);
      this.init(true);
    },
    changeBookSource() {
      this.saveReadingPosition({ immediate: true });
      this.popBookSourceVisible = false;
      this.show = false;
      this.startSavePosition = false;
      this.restoreReadingProgress = null;
      this.tryRefresh = false;
      // TODO 使用相似度比较，校正章节index
      this.init(true, true);
    },
    preserveCurrentReadingPosition() {
      const progress = this.saveReadingPosition({ immediate: true });
      this.startSavePosition = false;
      if (progress) {
        this.restoreReadingProgress = progress;
      }
      this.autoShowPosition();
    },
    loadCatalog(refresh, init) {
      if (!this.api) {
        setTimeout(() => {
          if (this.loadCatalog) {
            this.loadCatalog(refresh);
          }
        }, 1000);
        return;
      }
      if (refresh && this.startSavePosition && this.show) {
        this.preserveCurrentReadingPosition();
      }
      this.getCatalog(refresh).then(
        res => {
          if (res.data.isSuccess) {
            var book = Object.assign({}, this.$store.getters.readingBook);
            book.catalog = res.data.data;
            if (!book.catalog.length) {
              book.index = 0;
            } else {
              book.index = Math.min(
                Math.max(Number(book.index) || 0, 0),
                book.catalog.length - 1
              );
            }
            this.$store.commit("setReadingBook", book);
            this.$emit("loadCatalog");
            var index = book.index;
            if (
              this.restoreReadingProgress &&
              this.restoreReadingProgress.chapterIndex !== index
            ) {
              this.restoreReadingProgress = {
                ...this.restoreReadingProgress,
                chapterIndex: index,
                position: 0
              };
            }
            this.scrollStartChapterIndex = index;
            this.showPrevChapterSize = 0;
            this.showNextChapterSize = 1;
            this.getContent(index);
          } else {
            if (init) {
              this.title = "";
              this.content = "获取章节目录失败！\n" + res.data.errorMsg;
              this.error = true;
              this.show = true;
              this.$emit("showContent");
            }
            this.loading.close();
          }
        },
        error => {
          this.loading.close();
          this.$message.error(
            "获取书籍目录列表 " + (error && error.toString())
          );
        }
      );
    },
    getCatalog(refresh) {
      const params = {
        url: this.$store.getters.readingBook.bookUrl,
        refresh: refresh ? 1 : 0
      };
      if (this.$route.query.search) {
        // 来自搜索结果，请求需要带上书源和搜索阶段生成的解析变量。
        const readingBook = this.$store.getters.readingBook;
        params.bookSourceUrl = readingBook.origin;
        params.book = {
          bookUrl: readingBook.bookUrl,
          name: readingBook.name,
          author: readingBook.author,
          origin: readingBook.origin,
          originName: readingBook.originName,
          type: readingBook.type,
          coverUrl: readingBook.coverUrl,
          tocUrl: readingBook.tocUrl,
          intro: readingBook.intro,
          kind: readingBook.kind,
          wordCount: readingBook.wordCount,
          latestChapterTitle: readingBook.latestChapterTitle,
          variable: readingBook.variable,
          originOrder: readingBook.originOrder
        };
      }
      return networkFirstRequest(
        () => Axios.post(this.api + "/getChapterList", params),
        this.$store.getters.readingBook.name +
          "_" +
          this.$store.getters.readingBook.author +
          "@" +
          this.$store.getters.readingBook.bookUrl +
          "@chapterList"
      );
    },
    refreshCatalog() {
      return this.loadCatalog(true);
    },
    getBookContent(chapterIndex, options, refresh, cache) {
      return this.$root.$children[0].getBookContent(
        chapterIndex,
        options,
        refresh,
        cache
      );
    },
    refreshContent() {
      this.getContent(this.$store.getters.readingBook.index, true);
    },
    getContent(index, refresh) {
      const changingChapter = index !== this.chapterIndex;
      const refreshingChapter =
        this.startSavePosition && !!refresh && index === this.chapterIndex;
      if (changingChapter) {
        if (this.startSavePosition) {
          this.saveReadingPosition({ immediate: true });
        }
        this.startSavePosition = false;
        this.readingProgressInitId++;
        this.restoreReadingProgress = null;
        this.$once("showContent", () => {
          this.$nextTick(() => {
            this.startSavePosition = true;
            this.saveReadingPosition({ immediate: true });
          });
        });
      } else if (refreshingChapter) {
        this.preserveCurrentReadingPosition();
      }
      //展示进度条
      this.show = false;
      if (!this.loading || !this.loading.visible) {
        this.loading = this.$loading({
          target: this.$refs.content,
          lock: true,
          text: refresh ? "正在刷新内容" : "正在获取内容",
          spinner: "el-icon-loading",
          background: "rgba(0,0,0,0)"
        });
      }
      let bookUrl = this.$store.getters.readingBook.bookUrl;
      try {
        // 保存阅读进度
        let book = { ...this.$store.getters.readingBook };
        book.index = index;
        this.$store.commit("setReadingBook", book);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
      //强制滚回顶层
      this.toTop(0);
      // 如果超出目录范围，尝试刷新目录
      if (!this.$store.getters.readingBook.catalog[index]) {
        if (this.tryRefresh) {
          this.tryRefresh = false;
          this.content = "获取章节内容失败，请更新目录！";
          this.error = true;
          this.show = true;
          this.$emit("showContent");
          this.loading.close();
        } else {
          this.tryRefresh = true;
          this.refreshCatalog();
        }
        return;
      }
      //let chapterUrl = this.$store.getters.readingBook.catalog[index].url;
      let chapterName = this.$store.getters.readingBook.catalog[index].title;
      let chapterIndex = this.$store.getters.readingBook.catalog[index].index;
      this.title = chapterName;
      this.getBookContent(chapterIndex, {}, refresh).then(
        res => {
          if (
            bookUrl !== this.$store.getters.readingBook.bookUrl ||
            index !== this.$store.getters.readingBook.index
          ) {
            // 已经换书或者换章节了
            return;
          }
          if (res.data.isSuccess) {
            let data = res.data.data;
            this.content = this.filterContent(data);
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: res.data.data,
              error: false
            });
            this.loading.close();
            this.error = false;
            this.show = true;
            this.$emit("showContent");
          } else {
            this.content = "获取章节内容失败！\n" + res.data.errorMsg;
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: "获取章节内容失败！\n" + res.data.errorMsg,
              error: true
            });
            this.error = true;
            this.show = true;
            this.$emit("showContent");
            this.loading.close();
          }
          if (this.isScrollRead) {
            this.computeShowChapterList();
          }
        },
        error => {
          if (
            bookUrl !== this.$store.getters.readingBook.bookUrl ||
            index !== this.$store.getters.readingBook.index
          ) {
            // 已经换书或者换章节了
            return;
          }
          this.content = "获取章节内容失败！\n" + (error && error.toString());
          this.addChapterContentToCache({
            bookUrl,
            index: index,
            title: chapterName,
            content: "获取章节内容失败！\n" + (error && error.toString()),
            error: true
          });
          this.error = true;
          this.show = true;
          this.$emit("showContent");
          this.loading.close();
          this.$message.error(
            "获取章节内容失败 " + (error && error.toString())
          );
          if (this.isScrollRead) {
            this.computeShowChapterList();
          }
          throw error;
        }
      );
    },
    filterContent(content) {
      if (this.isEpubIframeContent(content) || this.isAudio) {
        return content;
      }
      if (!content) {
        return content;
      }
      content = applyReplaceRulesToText(
        content,
        this.filterRules,
        this.$store.getters.readingBook
      );
      content.replace(/\\n+/g, "\n");
      content = this.formatChinese(content);
      return content;
    },
    loadShowChapter(index, refresh) {
      if (
        !refresh &&
        this.chapterContentCache &&
        this.chapterContentCache.chapters[index] &&
        !this.chapterContentCache.chapters[index].error
      ) {
        if (
          index >= this.chapterIndex - this.showPrevChapterSize &&
          index <= this.chapterIndex + this.showNextChapterSize
        ) {
          this.computeShowChapterList();
        }
        return Promise.resolve();
      }
      let bookUrl = this.$store.getters.readingBook.bookUrl;
      if (!this.$store.getters.readingBook.catalog) {
        return new Promise(resolve => {
          this.$once("loadCatalog", () => {
            this.loadShowChapter(index, refresh).then(resolve);
          });
        });
      }
      // 如果超出目录范围，尝试刷新目录
      if (!this.$store.getters.readingBook.catalog[index]) {
        return Promise.reject("章节不存在");
      }
      let chapterName = this.$store.getters.readingBook.catalog[index].title;
      let chapterIndex = this.$store.getters.readingBook.catalog[index].index;
      return this.getBookContent(chapterIndex, {}, refresh, true).then(
        res => {
          if (res.data.isSuccess) {
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: res.data.data,
              error: false
            });
          } else {
            this.addChapterContentToCache({
              bookUrl,
              index: index,
              title: chapterName,
              content: "获取章节内容失败！\n" + res.data.errorMsg,
              error: true
            });
          }
        },
        error => {
          this.addChapterContentToCache({
            bookUrl,
            index: index,
            title: chapterName,
            content: "获取章节内容失败！\n" + (error && error.toString()),
            error: true
          });
        }
      );
    },
    addChapterContentToCache(chapter) {
      if (
        !this.chapterContentCache ||
        this.chapterContentCache.bookUrl !== this.readingBook.bookUrl
      ) {
        this.chapterContentCache = {
          bookUrl: this.readingBook.bookUrl,
          chapters: {}
        };
      }
      if (
        typeof this.chapterContentCache.chapters[chapter.index] ===
          "undefined" || // 没有缓存
        !chapter.error || // 当前内容正确
        this.chapterContentCache.chapters[chapter.index].error // 缓存内容错误
      ) {
        // 查询是否卷名
        chapter.isVolume = !!(this.readingBook.catalog[chapter.index] || {})
          .isVolume;
        this.chapterContentCache.chapters[chapter.index] = chapter;
      }
    },
    computeShowChapterList(reset) {
      if (!this.chapterContentCache) {
        return new Promise(resolve => {
          setTimeout(() => {
            this.computeShowChapterList(reset).then(resolve);
          }, 10);
        });
      }
      if (!this.isScrollRead) {
        return Promise.resolve();
      }
      if (!this.catalog.length) {
        return Promise.resolve();
      }
      const list = [];
      let startIndex = Number.isFinite(this.scrollStartChapterIndex)
        ? this.scrollStartChapterIndex
        : this.chapterIndex;
      if (this.config.readMethod === "上下滚动2") {
        startIndex = this.chapterIndex - this.showPrevChapterSize;
      }
      startIndex = Math.max(0, startIndex);
      const endIndex = Math.min(
        this.catalog.length - 1,
        this.chapterIndex + this.showNextChapterSize
      );
      const waitPromise = [];
      for (let i = startIndex; i <= endIndex; i++) {
        if (!this.chapterContentCache.chapters[i]) {
          waitPromise.push(this.loadShowChapter(i));
          continue;
        }
        list.push({
          ...this.chapterContentCache.chapters[i],
          content: this.filterContent(
            this.chapterContentCache.chapters[i].content
          )
        });
      }
      if (waitPromise.length) {
        return Promise.all(waitPromise).then(() => {
          return this.computeShowChapterList(reset);
        });
      }
      this.saveReadingPosition();
      // 暂停记录位置
      this.startSavePosition = false;
      // 记录当前章节
      this.showChapterList = list;
      return new Promise(resolve => {
        this.$nextTick(() => {
          this.computePages(() => {
            if (reset) {
              // 切换上下章节，滚动到顶部
              this.toTop(0, () => {
                this.startSavePosition = true;
                this.saveReadingPosition({ immediate: true });
                resolve();
              });
              return;
            } else if (
              this.restoreReadingProgress &&
              this.restoreReadingProgress.bookUrl ===
                this.readingBook.bookUrl &&
              this.restoreReadingProgress.chapterIndex === this.chapterIndex
            ) {
              this.autoShowPosition(true);
            } else {
              this.startSavePosition = true;
            }
            resolve();
          });
        });
      });
    },
    saveBookProgress() {
      return this.saveReadingPosition({ immediate: true });
    },
    clearReadingProgressTimers() {
      if (this.readingProgressSyncTimer) {
        clearTimeout(this.readingProgressSyncTimer);
        this.readingProgressSyncTimer = null;
      }
      if (this.readingProgressMaxTimer) {
        clearTimeout(this.readingProgressMaxTimer);
        this.readingProgressMaxTimer = null;
      }
    },
    getReadingProgressQueueKey(progress) {
      return progress
        ? encodeURIComponent(
            progress.userName || this.$store.getters.currentUserName
          ) +
            "@" +
            encodeURIComponent(progress.bookUrl)
        : "";
    },
    enqueueReadingProgress(progress) {
      const key = this.getReadingProgressQueueKey(progress);
      if (!key) {
        return;
      }
      this.readingProgressQueue[key] = progress;
      this.pendingReadingProgress = progress;
    },
    takeNextReadingProgress(skippedKeys) {
      skippedKeys = skippedKeys || {};
      const pendingKey = this.getReadingProgressQueueKey(
        this.pendingReadingProgress
      );
      let key =
        pendingKey &&
        this.readingProgressQueue[pendingKey] &&
        this.readingProgressQueue[pendingKey] !== skippedKeys[pendingKey]
          ? pendingKey
          : Object.keys(this.readingProgressQueue).find(
              queueKey =>
                this.readingProgressQueue[queueKey] !== skippedKeys[queueKey]
            );
      if (!key) {
        return null;
      }
      const progress = this.readingProgressQueue[key];
      delete this.readingProgressQueue[key];
      const nextKey = Object.keys(this.readingProgressQueue)[0];
      this.pendingReadingProgress = nextKey
        ? this.readingProgressQueue[nextKey]
        : null;
      return progress;
    },
    scheduleReadingProgressSync(progress) {
      if (!progress || !this.isBookInShelf(progress.bookUrl)) {
        return;
      }
      this.enqueueReadingProgress(progress);
      if (this.readingProgressSyncTimer) {
        clearTimeout(this.readingProgressSyncTimer);
      }
      this.readingProgressSyncTimer = setTimeout(() => {
        this.readingProgressSyncTimer = null;
        this.flushReadingProgress();
      }, readingPositionDebounce);
      if (!this.readingProgressMaxTimer) {
        this.readingProgressMaxTimer = setTimeout(() => {
          this.readingProgressMaxTimer = null;
          this.flushReadingProgress(true);
        }, readingPositionMaxWait);
      }
    },
    normalizeSavedReadingProgress(data, fallback) {
      const progress = this.normalizeReadingProgress(data, {
        bookUrl: (fallback || {}).bookUrl,
        userName: (fallback || {}).userName
      });
      if (progress) {
        return progress;
      }
      return fallback
        ? {
            ...fallback,
            pending: false,
            legacy: false,
            confirmed: true,
            updatedAt: Date.now()
          }
        : null;
    },
    async flushReadingProgress(immediate, skippedKeys) {
      skippedKeys = skippedKeys || {};
      if (immediate) {
        this.clearReadingProgressTimers();
      }
      if (this.readingProgressRequest) {
        return this.readingProgressRequest;
      }
      const progress = this.takeNextReadingProgress(skippedKeys);
      if (!progress) {
        return null;
      }
      if (
        progress.userName !== this.$store.getters.currentUserName ||
        !this.isBookInShelf(progress.bookUrl)
      ) {
        return this.flushReadingProgress(true, skippedKeys);
      }
      const payload = {
        url: progress.bookUrl,
        index: progress.chapterIndex,
        position: progress.position,
        positionType: progress.positionType
      };
      let requestFailed = false;
      const cancelSource = Axios.createCancelTokenSource();
      this.readingProgressCancelSource = cancelSource;
      this.readingProgressInFlight = progress;
      this.readingProgressRequest = Axios.post(
        this.api + "/saveBookProgress",
        payload,
        { silent: true, cancelToken: cancelSource.token, timeout: 10000 }
      )
        .then(response => {
          if (!response || !response.data || !response.data.isSuccess) {
            throw new Error(
              (response && response.data && response.data.errorMsg) ||
                "保存阅读进度失败"
            );
          }
          const confirmed = this.normalizeSavedReadingProgress(
            response.data.data,
            progress
          );
          if (confirmed) {
            confirmed.userName = progress.userName;
            confirmed.confirmed = true;
            if (progress.userName === this.$store.getters.currentUserName) {
              this.$store.commit("setReadingProgress", {
                ...confirmed,
                setCurrentIndex: false
              });
            }
            const cached = this.getCachedReadingProgress({
              bookUrl: progress.bookUrl,
              userName: progress.userName
            });
            if (this.isReadingProgressEqual(cached, progress)) {
              this.setCachedReadingProgress(
                confirmed,
                false,
                confirmed.updatedAt
              );
            }
          }
          return confirmed;
        })
        .catch(() => {
          requestFailed = true;
          const failedKey = this.getReadingProgressQueueKey(progress);
          if (!this.readingProgressQueue[failedKey]) {
            this.enqueueReadingProgress(progress);
          }
          return null;
        })
        .finally(() => {
          this.readingProgressRequest = null;
          if (this.readingProgressCancelSource === cancelSource) {
            this.readingProgressCancelSource = null;
          }
          if (this.readingProgressInFlight === progress) {
            this.readingProgressInFlight = null;
          }
          const failedKey = this.getReadingProgressQueueKey(progress);
          if (
            requestFailed &&
            this.readingProgressQueue[failedKey] === progress
          ) {
            skippedKeys[failedKey] = progress;
          }
          const queuedKeys = Object.keys(this.readingProgressQueue);
          const nextKey = queuedKeys.find(
            key => this.readingProgressQueue[key] !== skippedKeys[key]
          );
          if (nextKey) {
            this.pendingReadingProgress = this.readingProgressQueue[nextKey];
            this.flushReadingProgress(true, skippedKeys);
          }
        });
      return this.readingProgressRequest;
    },
    retryPendingReadingProgress() {
      this.getPendingReadingProgressList().forEach(progress => {
        if (this.isBookInShelf(progress.bookUrl)) {
          this.enqueueReadingProgress(progress);
        }
      });
      if (Object.keys(this.readingProgressQueue).length) {
        this.flushReadingProgress(true);
      }
    },
    getReadingProgressBeaconUrl() {
      const params = [];
      if (this.$store.state.token) {
        params.push(
          "accessToken=" + encodeURIComponent(this.$store.state.token)
        );
      }
      if (this.$store.state.isManagerMode && this.$store.state.secureKey) {
        params.push(
          "secureKey=" + encodeURIComponent(this.$store.state.secureKey)
        );
        params.push(
          "userNS=" + encodeURIComponent(this.$store.state.userNS || "")
        );
      }
      return (
        this.api +
        "/saveBookProgress" +
        (params.length ? "?" + params.join("&") : "")
      );
    },
    sendReadingProgressBeacon(progress) {
      if (
        !progress ||
        progress.userName !== this.$store.getters.currentUserName ||
        !this.isBookInShelf(progress.bookUrl)
      ) {
        return false;
      }
      const body = JSON.stringify({
        url: progress.bookUrl,
        index: progress.chapterIndex,
        position: progress.position,
        positionType: progress.positionType
      });
      const url = this.getReadingProgressBeaconUrl();
      if (navigator.sendBeacon) {
        try {
          const queued = navigator.sendBeacon(
            url,
            new Blob([body], { type: "text/plain;charset=UTF-8" })
          );
          if (queued) {
            return true;
          }
        } catch (error) {
          // Fall through to a keepalive request.
        }
      }
      if (window.fetch) {
        try {
          window.fetch(url, {
            method: "POST",
            body,
            headers: { "Content-Type": "text/plain;charset=UTF-8" },
            credentials: "include",
            keepalive: true
          });
          return true;
        } catch (error) {
          return false;
        }
      }
      return false;
    },
    readingVisibilityChangeHandler() {
      if (document.visibilityState === "hidden") {
        this.saveReadingPosition({ immediate: true });
      } else {
        this.retryPendingReadingProgress();
      }
    },
    pageHideHandler() {
      const inFlightProgress = this.readingProgressInFlight;
      if (this.readingProgressCancelSource) {
        this.readingProgressCancelSource.cancel("pagehide");
      }
      const currentProgress = this.saveReadingPosition({ beacon: true });
      const currentKey = this.getReadingProgressQueueKey(currentProgress);
      const additionalProgress = {};
      [
        inFlightProgress,
        this.pendingReadingProgress,
        ...Object.keys(this.readingProgressQueue).map(
          key => this.readingProgressQueue[key]
        )
      ].forEach(progress => {
        const key = this.getReadingProgressQueueKey(progress);
        if (key && key !== currentKey) {
          additionalProgress[key] = progress;
        }
      });
      Object.keys(additionalProgress).forEach(key => {
        this.sendReadingProgressBeacon(additionalProgress[key]);
      });
    },
    toTop(interval, callback) {
      if (interval === 0) {
        document.documentElement.scrollTop = 0;
        document.body.scrollTop = 0;
        callback && callback();
        return;
      }
      if (this.$store.state.miniInterface) {
        this.scrollContent(
          -(document.documentElement.scrollTop || document.body.scrollTop),
          interval
        );
        callback && callback();
      } else {
        jump(this.$refs.top, { duration: interval, callback });
      }
    },
    toBottom(interval) {
      jump(this.$refs.bottom, { duration: interval });
    },
    toNextChapter(onError) {
      if (
        !this.$store.getters.readingBook ||
        !this.$store.getters.readingBook.bookUrl ||
        !this.$store.getters.readingBook.catalog
      ) {
        onError && onError();
        return;
      }
      let index = this.$store.getters.readingBook.index;
      index++;
      if (
        typeof this.$store.getters.readingBook.catalog[index] !== "undefined"
      ) {
        if (this.isScrollRead) {
          this.changeScrollChapter(index);
          return;
        }
        this.getContent(index);
      } else {
        onError && onError();
        this.$message.error("本章是最后一章");
      }
    },
    toLastChapter(onError) {
      if (
        !this.$store.getters.readingBook ||
        !this.$store.getters.readingBook.bookUrl ||
        !this.$store.getters.readingBook.catalog
      ) {
        onError && onError();
        return;
      }
      let index = this.$store.getters.readingBook.index;
      index--;
      if (
        typeof this.$store.getters.readingBook.catalog[index] !== "undefined"
      ) {
        if (this.isScrollRead) {
          this.changeScrollChapter(index);
          return;
        }
        this.getContent(index);
      } else {
        this.$message.error("本章是第一章");
        onError && onError();
      }
    },
    changeScrollChapter(index) {
      const book = this.$store.getters.readingBook || {};
      if (!book.bookUrl || !book.catalog || !book.catalog[index]) {
        return Promise.resolve();
      }
      this.saveReadingPosition({ immediate: true });
      this.startSavePosition = false;
      this.readingProgressInitId++;
      this.restoreReadingProgress = null;
      this.$store.commit("setReadingBookIndex", {
        bookUrl: book.bookUrl,
        chapterIndex: index
      });
      this.title = book.catalog[index].title || this.title;
      this.scrollStartChapterIndex = index;
      this.showPrevChapterSize = 0;
      this.showNextChapterSize = 1;
      return this.computeShowChapterList(true);
    },
    toShelf() {
      // Save while the current EPUB iframe still has its original layout. Starting
      // router navigation can reflow the iframe back to the top before leave hooks run.
      this.prepareToLeaveReader();
      this.$router.push("/");
    },
    computePages(cb) {
      if (!this.$refs.bookContentRef || !this.$refs.bookContentRef.$el) {
        setTimeout(() => {
          this.computePages(cb);
        }, 30);
        return;
      }
      if (this.isSlideRead) {
        this.totalPages = Math.ceil(
          this.$refs.bookContentRef.$el.scrollWidth /
            (this.windowSize.width - 16)
        );
      } else {
        this.totalPages = Math.ceil(
          this.$refs.bookContentRef.$el.scrollHeight /
            (this.windowSize.height - this.scrollOffset)
        );
      }
      if (this.showLastPage) {
        this.showPage(this.totalPages, 0);
        this.showLastPage = false;
      }
      cb && cb();
    },
    nextPage(moveX) {
      if (!this.show) {
        return;
      }
      if (this.transforming) {
        return;
      }
      if (this.isSlideRead) {
        if (this.currentPage < this.totalPages) {
          if (typeof moveX === "undefined") {
            this.transformX =
              -(this.windowSize.width - 16) * (this.currentPage - 1);
          }
          this.currentPage += 1;
          this.transforming = true;
          this.transform(
            typeof moveX === "undefined"
              ? -(this.windowSize.width - 16)
              : moveX,
            this.animateMSTime
          );
        } else {
          this.toNextChapter(() => {
            if (typeof moveX !== "undefined") {
              // 没有下一章，但是已经做了动画，恢复
              this.showPage(this.currentPage, 0);
            }
          });
        }
      } else {
        if (
          (document.documentElement.scrollTop || document.body.scrollTop) +
            this.windowSize.height <
          document.documentElement.scrollHeight
        ) {
          this.currentPage += 1;
          const moveY = this.windowSize.height - this.scrollOffset;
          this.transforming = true;
          this.scrollContent(moveY, this.animateMSTime);
        } else {
          this.currentPage = 1;
          this.toNextChapter();
        }
      }
    },
    prevPage(moveX) {
      if (!this.show) {
        return;
      }
      if (this.transforming) {
        return;
      }
      if (this.isSlideRead) {
        if (this.currentPage > 1) {
          if (typeof moveX === "undefined") {
            this.transformX =
              -(this.windowSize.width - 16) * (this.currentPage - 1);
          }
          this.currentPage -= 1;
          this.transforming = true;
          this.transform(
            typeof moveX === "undefined" ? this.windowSize.width - 16 : moveX,
            this.animateMSTime
          );
        } else {
          this.showLastPage = true;
          this.toLastChapter(() => {
            if (typeof moveX !== "undefined") {
              // 没有下一章，但是已经做了动画，恢复
              this.showPage(this.currentPage, 0);
            }
          });
        }
      } else {
        if (
          (document.documentElement.scrollTop || document.body.scrollTop) > 0
        ) {
          this.currentPage -= 1;
          const moveY = -this.windowSize.height + this.scrollOffset;
          this.transforming = true;
          this.scrollContent(moveY, this.animateMSTime);
        } else {
          this.currentPage = 1;
          this.toLastChapter();
        }
      }
    },
    showPage(page, duration) {
      if (!this.show) {
        return;
      }
      this.currentPage = Math.min(page, this.totalPages);
      if (this.isSlideRead) {
        const moveX =
          -(this.windowSize.width - 16) * (this.currentPage - 1) -
          this.transformX;
        this.transform(
          moveX,
          typeof duration === "undefined" ? this.animateMSTime : duration
        );
      } else {
        const moveY =
          (this.windowSize.height - 10) * (this.currentPage - 1) -
          (document.documentElement.scrollTop || document.body.scrollTop);
        this.scrollContent(
          moveY,
          typeof duration === "undefined" ? this.animateMSTime : duration
        );
      }
    },
    transform(moveX, duration) {
      const onEnd = () => {
        this.contentStyle = {
          transform: `translateX(${this.transformX + moveX}px)`
        };
        this.transformX += moveX;
        this.transforming = false;
        // 保存进度
        setTimeout(this.saveReadingPosition, duration);
      };
      if (!duration) {
        onEnd();
        return;
      }
      const timing = Animate.Utils.makeEaseInOut(
        Animate.Timings.power.bind(null, 3)
      );

      new Animate({
        duration: duration || 500,
        timing: timing,
        draw: progress => {
          this.contentStyle = {
            transform: `translateX(${this.transformX + moveX * progress}px)`
          };
        },
        onEnd
      });
    },
    scrollContent(moveY, duration, isAccurate) {
      // console.log("scrollContent", moveY);
      const lastScrollTop = isAccurate
        ? 0
        : document.documentElement.scrollTop || document.body.scrollTop;
      const onEnd = () => {
        document.documentElement.scrollTop = lastScrollTop + moveY;
        document.body.scrollTop = lastScrollTop + moveY;
        this.transforming = false;
        // 保存进度
        setTimeout(this.saveReadingPosition, duration);
      };
      if (!duration) {
        onEnd();
        return;
      }
      const timing = Animate.Utils.makeEaseInOut(
        Animate.Timings.power.bind(null, 3)
      );

      new Animate({
        duration: duration || 500,
        timing: timing,
        draw: progress => {
          document.documentElement.scrollTop = lastScrollTop + moveY * progress;
          document.body.scrollTop = lastScrollTop + moveY * progress;
        },
        onEnd
      });
    },
    handlerClick(e) {
      if (this.isEpub) {
        return;
      }
      if (!this.lastTouch && !this.ignoreNextClick) {
        this.eventHandler(e);
      }
      this.ignoreNextClick = false;
    },
    handleTouchStart(e) {
      this.lastSelection = this.checkSelection();
      if (this.lastSelection) {
        return;
      }
      if (this.isAudio) {
        return;
      }
      if (this.isEpub) {
        return;
      }
      // e.preventDefault();
      // e.stopPropagation();
      this.lastTouch = false;
      this.lastMoveX = false;
      if (e.touches && e.touches[0]) {
        this.lastTouch = e.touches[0];
      }
    },
    handleTouchMove(e) {
      if (this.checkSelection()) {
        return;
      }
      if (e.touches && e.touches[0] && this.lastTouch) {
        this.lastMoveY = e.touches[0].clientY - this.lastTouch.clientY;
        if (this.isSlideRead) {
          e.preventDefault();
          e.stopPropagation();
          const moveX = e.touches[0].clientX - this.lastTouch.clientX;
          this.contentStyle = {
            transform: `translateX(${this.transformX + moveX}px)`
          };
          this.lastMoveX = moveX;
        }
      }
    },
    handleTouchEnd() {
      if (this.checkSelection(true)) {
        return;
      }
      if (this.lastSelection) {
        setTimeout(() => {
          this.showSelectionActionMenu(this.lastSelection);
          this.lastSelection = false;
        }, 200);
        return;
      }
      if (this.lastMoveX) {
        this.transformX += this.lastMoveX;
        if (this.lastMoveX > 0) {
          // 上一页
          this.prevPage(this.windowSize.width - 16 - this.lastMoveX);
        } else {
          // 下一页
          this.nextPage(-(this.windowSize.width - 16) - this.lastMoveX);
        }
      } else if (Math.abs(this.lastMoveY) <= 3 && this.lastTouch) {
        this.eventHandler(this.lastTouch);
      }
      setTimeout(() => {
        this.lastTouch = false;
        this.lastMoveX = false;
        this.lastMoveY = false;
      }, 300);
    },
    handleSelectionEnd() {
      if (this.isEpub || this.isAudio) {
        return;
      }
      if (this.selectionCheckTimer) {
        clearTimeout(this.selectionCheckTimer);
      }
      this.selectionCheckTimer = setTimeout(() => {
        this.selectionCheckTimer = null;
        if (!this.shouldShowSelectionAction()) {
          return;
        }
        const text = this.getSelectedText();
        if (!text) {
          return;
        }
        this.ignoreNextClick = true;
        this.showSelectionActionMenu(text);
      }, 80);
    },
    epubClickHash(rect) {
      if (typeof rect.top !== "undefined") {
        this.scrollContent(
          rect.top -
            (this.$store.state.miniInterface
              ? this.getFirstParagraphPos().bottom
              : 0) -
            (window.webAppDistance | 0) -
            (this.$store.state.safeArea.top | 0),
          0,
          true
        );
      }
    },
    epubBeforeLocationChangeHandler() {
      this.saveReadingPosition({ immediate: true });
    },
    epubLocationChangeHandler(url) {
      const normalizePath = path => {
        let value = String(path || "");
        if (/^(?:[a-z]+:)?\/\//i.test(value)) {
          const anchor = document.createElement("a");
          anchor.href = value;
          value = anchor.pathname;
        }
        value = value.split("#")[0].split("?")[0];
        try {
          value = decodeURIComponent(value);
        } catch (error) {
          // Keep the original path when it contains malformed escapes.
        }
        return value.replace(/\\/g, "/").replace(/^\.\//, "");
      };
      const targetPath = normalizePath(url);
      // 判断是否跳转了其他章节
      const currentChapter = this.catalog[this.chapterIndex];
      if (currentChapter) {
        const contentPath = normalizePath(this.content);
        let chapterPrefix = "";
        let longestContentMatch = "";
        for (let i = 0; i < this.catalog.length; i++) {
          const catalogPath = normalizePath(this.catalog[i].url).replace(
            /^\/+/,
            ""
          );
          if (
            catalogPath.length > longestContentMatch.length &&
            (contentPath === catalogPath ||
              contentPath.endsWith("/" + catalogPath))
          ) {
            longestContentMatch = catalogPath;
          }
        }
        if (longestContentMatch) {
          chapterPrefix = contentPath.slice(
            0,
            contentPath.length - longestContentMatch.length
          );
        }
        const iframeUrlPath =
          chapterPrefix && targetPath.indexOf(chapterPrefix) === 0
            ? targetPath.slice(chapterPrefix.length)
            : targetPath.replace(/^\/+/, "");
        let newChapterIndex = -1;
        let longestTargetMatch = -1;
        for (let i = 0; i < this.catalog.length; i++) {
          const catalogPath = normalizePath(this.catalog[i].url).replace(
            /^\/+/,
            ""
          );
          if (catalogPath === iframeUrlPath.replace(/^\/+/, "")) {
            newChapterIndex = i;
            break;
          }
          if (
            catalogPath.length > longestTargetMatch &&
            targetPath.endsWith("/" + catalogPath)
          ) {
            newChapterIndex = i;
            longestTargetMatch = catalogPath.length;
          }
        }
        if (newChapterIndex >= 0 && newChapterIndex !== this.chapterIndex) {
          const bookUrl = this.readingBook.bookUrl;
          const initId = this.readingProgressInitId;
          this.startSavePosition = false;
          let book = { ...this.$store.getters.readingBook };
          book.index = newChapterIndex;
          this.$store.commit("setReadingBook", book);
          this.title = this.$store.getters.readingBook.catalog[
            newChapterIndex
          ].title;
          const progress = {
            bookUrl,
            userName: this.$store.getters.currentUserName || "default",
            chapterIndex: newChapterIndex,
            chapterTitle: this.title,
            position: 0,
            positionType: readingPositionTypes.ratio,
            updatedAt: Date.now(),
            pending: this.isBookInShelf(bookUrl),
            legacy: false
          };
          this.restoreReadingProgress = progress;
          this.$once("iframeLoad", () => {
            if (
              initId !== this.readingProgressInitId ||
              bookUrl !== this.readingBook.bookUrl ||
              newChapterIndex !== this.chapterIndex
            ) {
              return;
            }
            this.showVisualReadingPosition(progress, () => {
              this.startSavePosition = true;
              this.saveReadingPosition({ immediate: true });
            });
          });
        }
      }
    },
    handleEpubSelection(selection) {
      if (!this.shouldShowSelectionAction()) {
        return;
      }
      if (!selection || !selection.text) {
        this.hideSelectionToolbar(false);
        return;
      }
      this.ignoreNextClick = true;
      this.showSelectionActionMenu(selection.text, selection.rect);
    },
    eventHandler(point) {
      // console.log(point);
      if (this.suppressSelectionToolbarTap) {
        this.suppressSelectionToolbarTap = false;
        return;
      }
      if (this.isEpub && this.ignoreNextClick) {
        this.ignoreNextClick = false;
        return;
      }
      if (
        this.isEpub &&
        this.selectionToolbarVisible &&
        new Date().getTime() - this.selectionToolbarShownAt < 800
      ) {
        return;
      }
      if (this.isEpub && this.selectionToolbarVisible) {
        this.hideSelectionToolbar(false);
        return;
      }
      if (!this.isEpub && this.checkSelection(true)) {
        // 选择文本
        this.ignoreNextClick = true;
        return;
      }
      if (
        this.popBookSourceVisible ||
        this.popBookShelfVisible ||
        this.popCataVisible ||
        this.readSettingsVisible ||
        this.dictionaryVisible
      ) {
        if (this.isEpub) {
          this.popBookSourceVisible = false;
          this.popBookShelfVisible = false;
          this.popCataVisible = false;
          this.readSettingsVisible = false;
        }
        return;
      }
      if (this.isAudio) {
        // 音频
        // 点击中部区域显示菜单
        if (!this.showReadBar) {
          this.showToolBar = !this.showToolBar;
        }
        return;
      }
      if (this.autoReading) {
        this.showToolBar = !this.showToolBar;
        return;
      }
      // 根据点击位置判断操作
      const midX = this.windowSize.width / 2;
      const midY = this.windowSize.height / 2;
      if (this.isEpub) {
        point.clientY =
          point.clientY +
          45 -
          (document.documentElement.scrollTop || document.body.scrollTop);
      }
      if (
        Math.abs(point.clientY - midY) <= this.windowSize.height * 0.2 &&
        Math.abs(point.clientX - midX) <= this.windowSize.width * 0.2
      ) {
        // 点击中部区域显示菜单
        if (!this.showReadBar) {
          this.showToolBar = !this.showToolBar;
        }
      } else if (this.$store.getters.config.clickMethod === "下一页") {
        // 全屏点击下一页
        this.showToolBar = false;
        this.nextPage();
        return;
      } else if (this.$store.getters.config.clickMethod === "不翻页") {
        // 全屏点击不翻页
        this.showToolBar = !this.showToolBar;
        return;
      } else if (this.isSlideRead) {
        if (point.clientX > midX) {
          // 点击右侧，下一页
          this.showToolBar = false;
          this.nextPage();
        } else if (point.clientX < midX) {
          // 点击左侧，上一页
          this.showToolBar = false;
          this.prevPage();
        }
      } else {
        if (point.clientY > midY) {
          // 点击下部，下一页
          this.showToolBar = false;
          this.nextPage();
        } else if (point.clientY < midY) {
          // 点击上部，上一页
          this.showToolBar = false;
          this.prevPage();
        }
      }
    },
    keydownHandler(event, force) {
      // console.log("keyup", event);
      const keyCodeMap = {
        37: "ArrowLeft",
        38: "ArrowUp",
        39: "ArrowRight",
        40: "ArrowDown",
        27: "Escape"
      };
      const eventKey = event.key || keyCodeMap[event.keyCode];
      if (this.selectionToolbarVisible) {
        if (eventKey === "Escape") {
          this.hideSelectionToolbar(true);
        }
        return;
      }
      if (
        this.popBookSourceVisible ||
        this.popBookShelfVisible ||
        this.popCataVisible ||
        this.readSettingsVisible ||
        this.dictionaryVisible ||
        this.selectionActionPrompting ||
        this.showTextFilterPrompting
      ) {
        return;
      }
      if (!force && document.activeElement !== document.body) {
        return;
      }
      if (this.isAudio) {
        return;
      }
      switch (eventKey) {
        case "ArrowLeft":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          if (this.isSlideRead) {
            this.prevPage();
          } else {
            this.toLastChapter();
          }
          break;
        case "ArrowRight":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          if (this.isSlideRead) {
            this.nextPage();
          } else {
            this.toNextChapter();
          }
          break;
        case "ArrowUp":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          this.prevPage();
          break;
        case "ArrowDown":
          event.preventDefault && event.preventDefault();
          event.stopPropagation && event.stopPropagation();
          this.showToolBar = false;
          this.nextPage();
          break;
        case "Escape":
          this.toShelf();
          break;
      }
    },
    formatProgressTip(value) {
      return `第 ${value || this.progressValue}/${this.totalPages} 页`;
    },
    formatTime() {
      const now = new Date();
      const pad = v => (v >= 10 ? "" + v : "0" + v);
      this.timeStr = pad(now.getHours()) + ":" + pad(now.getMinutes());
    },
    checkSelection(show) {
      const text = this.getSelectedText();
      if (text && show) {
        setTimeout(() => {
          if (this.shouldShowSelectionAction()) {
            this.showSelectionActionMenu(text);
          }
        }, 200);
      } else if (!text && show) {
        this.hideSelectionToolbar(false);
      }
      return text;
    },
    getSelectedText() {
      if (window.getSelection) {
        return window.getSelection().toString();
      }
      if (document.selection && document.selection.type != "Control") {
        return document.selection.createRange().text;
      }
      return "";
    },
    shouldShowSelectionAction() {
      return this.$store.getters.config.selectionAction !== "忽略";
    },
    clearTextSelection() {
      try {
        if (window.getSelection) {
          window.getSelection().removeAllRanges();
        } else if (document.selection) {
          document.selection.empty();
        }
        if (
          this.isEpub &&
          this.$refs.bookContentRef &&
          this.$refs.bookContentRef.clearIframeSelection
        ) {
          this.$refs.bookContentRef.clearIframeSelection();
        }
      } catch (error) {
        //
      }
    },
    async showSelectionActionMenu(text, rect) {
      const pureText = (text || "").replace(/^\s+/, "").replace(/\s+$/, "");
      if (!pureText) {
        this.hideSelectionToolbar(false);
        return;
      }
      const targetRect = rect || this.getSelectionRect();
      this.selectionToolbarText = pureText;
      this.selectionToolbarStyle = {
        left: "50%",
        top: "0px",
        opacity: 0
      };
      this.selectionToolbarVisible = true;
      this.selectionToolbarShownAt = new Date().getTime();
      this.$nextTick(() => {
        this.positionSelectionToolbar(targetRect);
      });
    },
    getSelectionRect() {
      try {
        if (window.getSelection) {
          const selection = window.getSelection();
          if (!selection || !selection.rangeCount) {
            return null;
          }
          const range = selection.getRangeAt(0);
          const rects = Array.prototype.slice
            .call(range.getClientRects())
            .filter(rect => rect.width || rect.height);
          if (!rects.length) {
            const rect = range.getBoundingClientRect();
            return rect.width || rect.height ? rect : null;
          }
          return rects.reduce(
            (result, rect) => ({
              top: Math.min(result.top, rect.top),
              right: Math.max(result.right, rect.right),
              bottom: Math.max(result.bottom, rect.bottom),
              left: Math.min(result.left, rect.left),
              width:
                Math.max(result.right, rect.right) -
                Math.min(result.left, rect.left),
              height:
                Math.max(result.bottom, rect.bottom) -
                Math.min(result.top, rect.top)
            }),
            {
              top: rects[0].top,
              right: rects[0].right,
              bottom: rects[0].bottom,
              left: rects[0].left,
              width: rects[0].width,
              height: rects[0].height
            }
          );
        }
        if (document.selection && document.selection.type != "Control") {
          return document.selection.createRange().getBoundingClientRect();
        }
      } catch (error) {
        //
      }
      return null;
    },
    positionSelectionToolbar(rect) {
      if (!this.selectionToolbarVisible) {
        return;
      }
      const toolbar = this.$refs.selectionToolbar;
      const viewportWidth = window.innerWidth || this.windowSize.width;
      const viewportHeight = window.innerHeight || this.windowSize.height;
      const margin = 8;
      const toolbarWidth = toolbar
        ? toolbar.offsetWidth
        : Math.min(360, viewportWidth - margin * 2);
      const toolbarHeight = toolbar ? toolbar.offsetHeight : 42;
      const targetRect = rect || {
        left: viewportWidth / 2,
        right: viewportWidth / 2,
        top: viewportHeight / 2,
        bottom: viewportHeight / 2,
        width: 0,
        height: 0
      };
      const centerX =
        targetRect.left +
        (targetRect.width || targetRect.right - targetRect.left) / 2;
      const minLeft = margin + toolbarWidth / 2;
      const maxLeft = viewportWidth - margin - toolbarWidth / 2;
      const left =
        maxLeft > minLeft
          ? Math.min(Math.max(centerX, minLeft), maxLeft)
          : viewportWidth / 2;
      let top = targetRect.top - toolbarHeight - 10;
      if (top < margin) {
        top = targetRect.bottom + 10;
      }
      if (top + toolbarHeight > viewportHeight - margin) {
        top = Math.max(margin, viewportHeight - toolbarHeight - margin);
      }
      this.selectionToolbarStyle = {
        left: left + "px",
        top: top + "px",
        opacity: 1
      };
    },
    hideSelectionToolbar(clearSelection) {
      this.selectionToolbarVisible = false;
      this.selectionToolbarText = "";
      this.selectionToolbarStyle = {};
      this.selectionToolbarShownAt = 0;
      this.selectionActionPrompting = false;
      if (clearSelection === true) {
        this.clearTextSelection();
      }
    },
    selectionOutsideHandler(event) {
      if (!this.selectionToolbarVisible) {
        return;
      }
      const toolbar = this.$refs.selectionToolbar;
      if (toolbar && toolbar.contains(event.target)) {
        return;
      }
      if (this.$refs.content && this.$refs.content.contains(event.target)) {
        this.suppressSelectionToolbarTap = true;
        this.ignoreNextClick = true;
      }
      this.hideSelectionToolbar(false);
    },
    runSelectionToolbarAction(action) {
      const text = this.selectionToolbarText;
      if (!text) {
        this.hideSelectionToolbar(true);
        return;
      }
      this.hideSelectionToolbar(true);
      switch (action) {
        case "dictionary":
          this.lookupSelectedText(text);
          break;
        case "filter":
          this.showTextFilterPrompt(text);
          break;
        case "bookmark":
          this.showAddBookmark(text);
          break;
        case "search":
          this.searchSelectedText(text);
          break;
        case "copy":
          this.copySelectionText(text);
          break;
      }
    },
    async copySelectionText(text) {
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(text);
        } else {
          const textarea = document.createElement("textarea");
          textarea.value = text;
          textarea.style.position = "fixed";
          textarea.style.opacity = "0";
          document.body.appendChild(textarea);
          textarea.select();
          document.execCommand("copy");
          document.body.removeChild(textarea);
        }
        this.$message.success("已复制");
      } catch (error) {
        this.$message.error("复制失败");
      }
    },
    searchSelectedText(text) {
      this.showSearchBookContentDialog(text);
    },
    normalizeDictionaryText(text) {
      return (text || "")
        .replace(/\s+/g, " ")
        .replace(/^\s+/, "")
        .replace(/\s+$/, "");
    },
    createEmptyDictionaryEntry(text) {
      return {
        text,
        query: text,
        title: "",
        isFallback: false,
        phonetics: [],
        entries: []
      };
    },
    getChineseDictionaryQuery(text) {
      return this.normalizeDictionaryText(text).replace(
        /[^\u3400-\u9fff]/g,
        ""
      );
    },
    async lookupSelectedText(text) {
      const query = this.getChineseDictionaryQuery(text);
      if (!query) {
        this.$message.error("中文字典仅支持中文词语");
        return;
      }
      if (query.length > 20) {
        this.$message.error("选择内容过长，请选择中文词语或短句");
        return;
      }
      const lookupWord = traditionalized(query);
      const cacheKey = lookupWord + "@" + this.config.chineseFont;
      this.dictionaryVisible = true;
      this.dictionaryError = "";
      this.dictionaryEntry = this.createEmptyDictionaryEntry(query);
      this.dictionaryLoading = false;
      if (this.dictionaryCache[cacheKey]) {
        this.dictionaryEntry = { ...this.dictionaryCache[cacheKey] };
        return;
      }

      const lookupId = new Date().getTime();
      this.dictionaryLookupId = lookupId;
      this.dictionaryLoading = true;
      const entry = await this.fetchChineseDictionary(query, lookupWord).catch(
        () => null
      );
      if (this.dictionaryLookupId !== lookupId) {
        return;
      }
      if (entry && entry.entries.length) {
        this.dictionaryEntry = entry;
        this.$set(this.dictionaryCache, cacheKey, { ...entry });
      } else {
        this.dictionaryEntry = this.createEmptyDictionaryEntry(query);
        this.dictionaryError = "未查到中文释义，请尝试选择更短的词语";
      }
      this.dictionaryLoading = false;
    },
    async fetchJson(url, timeout) {
      timeout = timeout || 10000;
      if (!window.AbortController) {
        const response = await fetch(url);
        if (!response.ok) {
          throw new Error("Request failed");
        }
        return await response.json();
      }
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), timeout);
      try {
        const response = await fetch(url, { signal: controller.signal });
        if (!response.ok) {
          throw new Error("Request failed");
        }
        return await response.json();
      } finally {
        clearTimeout(timer);
      }
    },
    async fetchChineseDictionary(query, lookupWord) {
      const directEntry = await this.fetchMoedictEntry(query, lookupWord);
      if (directEntry && directEntry.entries.length) {
        return directEntry;
      }

      const suggestionData = await this.fetchJson(
        "https://www.moedict.tw/" + encodeURIComponent(lookupWord) + ".json"
      );
      const terms = (suggestionData.terms || [])
        .map(term => traditionalized(this.stripDictionaryMarkup(term)))
        .filter(term => term)
        .slice(0, 4);
      if (!terms.length) {
        return null;
      }
      const entries = await Promise.all(
        terms.map(term => this.fetchMoedictEntry(query, term).catch(() => null))
      );
      const fallbackEntry = this.createEmptyDictionaryEntry(query);
      fallbackEntry.title = this.formatDictionaryText(lookupWord);
      fallbackEntry.isFallback = true;
      entries
        .filter(entry => entry && entry.entries.length)
        .forEach(entry => {
          fallbackEntry.phonetics = fallbackEntry.phonetics.concat(
            entry.phonetics
          );
          fallbackEntry.entries = fallbackEntry.entries.concat(entry.entries);
        });
      fallbackEntry.phonetics = Array.from(new Set(fallbackEntry.phonetics));
      return fallbackEntry;
    },
    async fetchMoedictEntry(query, lookupWord) {
      const data = await this.fetchJson(
        "https://www.moedict.tw/uni/" + encodeURIComponent(lookupWord) + ".json"
      );
      return this.parseMoedictEntry(query, data);
    },
    parseMoedictEntry(query, data) {
      if (!data || !data.title || !Array.isArray(data.heteronyms)) {
        return null;
      }
      const title = this.formatDictionaryText(
        this.stripDictionaryMarkup(data.title)
      );
      const phonetics = [];
      const entries = [];
      data.heteronyms.forEach(heteronym => {
        const pinyin = this.formatDictionaryText(
          this.stripDictionaryMarkup(heteronym.pinyin || "")
        );
        const bopomofo = this.stripDictionaryMarkup(heteronym.bopomofo || "");
        const phonetic = [pinyin, bopomofo].filter(v => v).join(" / ");
        if (phonetic && phonetics.indexOf(phonetic) < 0) {
          phonetics.push(phonetic);
        }
        const definitions = (heteronym.definitions || [])
          .map(definition => ({
            type: this.formatDictionaryText(
              this.stripDictionaryMarkup(definition.type || "")
            ),
            def: this.formatDictionaryText(
              this.stripDictionaryMarkup(definition.def || "")
            ),
            examples: this.formatDictionaryText(
              this.joinDictionaryText(definition.example)
            ),
            quotes: this.formatDictionaryText(
              this.joinDictionaryText(definition.quote)
            ),
            synonyms: this.formatDictionaryText(
              this.stripDictionaryMarkup(definition.synonyms || "")
            ),
            antonyms: this.formatDictionaryText(
              this.stripDictionaryMarkup(definition.antonyms || "")
            )
          }))
          .filter(definition => definition.def)
          .slice(0, 8);
        if (definitions.length) {
          entries.push({
            title,
            pinyin,
            bopomofo,
            definitions
          });
        }
      });
      return {
        text: query,
        query,
        title,
        isFallback: false,
        phonetics,
        entries
      };
    },
    joinDictionaryText(value) {
      if (Array.isArray(value)) {
        return value.map(v => this.stripDictionaryMarkup(v)).join(" ");
      }
      return this.stripDictionaryMarkup(value || "");
    },
    stripDictionaryMarkup(text) {
      const div = document.createElement("div");
      div.innerHTML = String(text || "");
      return (div.textContent || div.innerText || "")
        .replace(/[`~]/g, "")
        .replace(/\s+/g, " ")
        .replace(/^\s+/, "")
        .replace(/\s+$/, "");
    },
    formatDictionaryText(text) {
      if (!text) {
        return "";
      }
      return this.config.chineseFont === "简体" ? simplized(text) : text;
    },
    playDictionaryPronunciation() {
      const text =
        this.dictionaryEntry.title ||
        this.dictionaryEntry.text ||
        this.dictionaryEntry.query;
      if (!text) {
        return;
      }
      this.speakDictionaryText(text);
    },
    speakDictionaryText(text) {
      if (!this.speechAvalable) {
        this.$message.error("当前浏览器不支持朗读");
        return;
      }
      if (this.speechSpeaking) {
        this.stopSpeech();
      } else {
        window.speechSynthesis.cancel();
      }
      const utterance = new SpeechSynthesisUtterance(text);
      const language = "zh-CN";
      const languagePrefix = language.split("-")[0];
      const voice =
        this.voiceList.find(v => v.lang === language) ||
        this.voiceList.find(
          v => v.lang && v.lang.indexOf(languagePrefix) === 0
        );
      if (voice) {
        utterance.voice = voice;
      }
      utterance.lang = voice ? voice.lang : language;
      utterance.rate = this.speechRate || 1;
      utterance.pitch = this.speechPitch || 1;
      window.speechSynthesis.speak(utterance);
    },
    getUniqueReplaceRuleName(text) {
      const textPreview =
        text
          .replace(/\s+/g, " ")
          .replace(/^\s+/, "")
          .replace(/\s+$/, "")
          .slice(0, 12) || "选中文字";
      const baseName = `文本过滤-${textPreview}`;
      const ruleNameSet = new Set(this.filterRules.map(rule => rule.name));
      if (!ruleNameSet.has(baseName)) {
        return baseName;
      }
      let index = 2;
      while (ruleNameSet.has(`${baseName}-${index}`)) {
        index += 1;
      }
      return `${baseName}-${index}`;
    },
    async showTextFilterPrompt(text) {
      if (this.showTextFilterPrompting) {
        return;
      }
      if (!text.replace(/^\s+/, "").replace(/\s+$/, "")) {
        return;
      }

      const replaceRule = Object.assign({}, defaultReplaceRule, {
        name: this.getUniqueReplaceRuleName(text),
        pattern: text,
        replacement: "",
        isRegex: false,
        isEnabled: true,
        scope:
          this.$store.getters.readingBook.name +
          ";" +
          this.$store.getters.readingBook.bookUrl
      });
      this.showTextFilterPrompting = true;
      eventBus.$emit("showReplaceRuleForm", replaceRule, true, () => {
        this.showTextFilterPrompting = false;
      });
      // const h = this.$createElement;
      // const bgColor = this.isNight ? "#121212" : "#eee";
      // const preEle = h(
      //   "pre",
      //   {
      //     key: "" + new Date().getTime(),
      //     attrs: {
      //       contenteditable: "true"
      //     },
      //     style: `margin-top: 10px;background: ${bgColor};padding: 10px;border: 1px solid ${bgColor};border-radius: 5px;white-space: pre-wrap;word-wrap: break-word;word-break: break-all;`
      //   },
      //   text
      // );
      // const result = await this.$prompt(
      //   h("div", null, [
      //     h("p", null, "是否要将下列文字替换为输入内容:"),
      //     preEle
      //   ]),
      //   "操作确认",
      //   {
      //     inputPlaceholder: "留空为过滤"
      //   }
      // ).catch(() => {});
      // if (result && result.action === "confirm") {
      //   text = ((preEle.elm || {}).innerText || "")
      //     .replace(/^\s+/, "")
      //     .replace(/\s+$/, "");
      //   if (text) {
      //     this.$store.commit("addFilterRule", {
      //       name: "文本替换",
      //       pattern: text,
      //       replacement: result.value || "",
      //       isRegex: false,
      //       isEnabled: true,
      //       scope:
      //         this.$store.getters.readingBook.name +
      //         ";" +
      //         this.$store.getters.readingBook.bookUrl
      //     });
      //   } else {
      //     this.$message.error("过滤内容为空!");
      //   }
      // }
      // this.showTextFilterPrompting = false;
    },
    async showAddBookmark(text) {
      if (this.showAddBookmarking) {
        return;
      }
      let pureText = text.replace(/^\s+/, "").replace(/\s+$/, "");
      // console.log(pureText);
      const paragraph = this.getContentMatchParagraph(pureText, 1, 0.7);
      if (!paragraph) {
        this.$message.error("选择1-2段整段文字才能定位段落");
        return;
      }
      const paragraphLength = 5;
      const paragraphTextLength = 150;
      const paragraphList = [paragraph];
      let bookText = paragraph.innerText + "\n";
      if (
        paragraphList.length < paragraphLength &&
        bookText.length < paragraphTextLength
      ) {
        // 补全内容
        let paragraphIndex = -1;
        const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
        for (let i = 0; i < list.length; i++) {
          if (paragraphIndex > 0 && i > paragraphIndex) {
            paragraphList.push(list[i]);
            bookText += list[i].innerText + "\n";
            if (
              paragraphList.length >= paragraphLength ||
              bookText.length >= paragraphTextLength
            ) {
              break;
            }
          } else if (paragraphList[paragraphList.length - 1] === list[i]) {
            paragraphIndex = i;
          }
        }
      }
      // console.log(paragraphList, bookText);
      bookText = bookText.replace(/\\n*$/, "");

      const bookmark = Object.assign({}, defaultBookmark, {
        bookName: this.$store.getters.readingBook.name,
        bookAuthor: this.$store.getters.readingBook.author,
        chapterIndex: this.chapterIndex,
        chapterPos: this.currentPage,
        chapterName: this.title,
        bookText: bookText,
        content: ""
      });
      this.showAddBookmarking = true;
      eventBus.$emit("showBookmarkForm", bookmark, true, () => {
        this.showAddBookmarking = false;
      });
    },
    toogleNight() {
      if (this.isNight) {
        this.$store.commit("setNightTheme", false);
      } else {
        this.$store.commit("setNightTheme", true);
      }
    },
    fetchVoiceList() {
      this.voiceList = window.speechSynthesis.getVoices().sort((a, b) => {
        if (a.lang.startsWith("zh-") && b.lang.startsWith("zh-")) {
          return a.lang > b.lang ? 1 : a.lang < b.lang ? -1 : 0;
        } else if (a.lang.startsWith("zh-")) {
          return -1;
        } else if (b.lang.startsWith("zh-")) {
          return 1;
        }
        return a.lang > b.lang ? 1 : a.lang < b.lang ? -1 : 0;
      });
    },
    changeSpeechRate(rate) {
      this.speechRate = rate;
    },
    changeSpeechPitch(pitch) {
      this.speechPitch = pitch;
    },
    changeSpeechMinutes(minute) {
      this.speechMinutes = minute;
      if (minute) {
        this.speechEndTime = new Date().getTime() + minute * 60 * 1000;
      } else {
        this.speechEndTime = 0;
      }
    },
    startSpeech() {
      if (this.error) {
        return;
      }
      if (!this.voiceName) {
        return;
      }
      const voice = this.voiceList.find(v => v.name === this.voiceName);
      if (!voice) {
        return;
      }

      if (window.speechSynthesis.speaking) {
        return;
      }

      if (this.speechSpeaking) {
        if (
          this.speechEndTime > 0 &&
          new Date().getTime() > this.speechEndTime
        ) {
          this.$message.info("定时关闭朗读");
          return;
        }
      }

      const paragraph = this.getCurrentParagraph();
      if (!paragraph.innerText) {
        this.speechNext();
        return;
      }
      this.utterance = new SpeechSynthesisUtterance(paragraph.innerText);

      this.utterance.onstart = () => {
        this.speechSpeaking = true;
        this.skipAutoNext = false;
      };
      this.utterance.onend = () => {
        // 下一段
        if (!this.skipAutoNext) {
          this.speechNext();
        } else {
          this.skipAutoNext = false;
          this.speechSpeaking = false;
        }
      };
      this.utterance.onerror = event => {
        if (event.error || event.name) {
          this.$message.error(
            `朗读错误:  ${event.type || ""}  ${event.error ||
              event.name ||
              event.toString()}`
          );
        }
        this.speechSpeaking = window.speechSynthesis.speaking || false;
      };
      this.utterance.voice = voice;
      this.utterance.pitch = this.speechPitch;
      this.utterance.rate = this.speechRate;

      this.showParagraph(paragraph, true);
      paragraph.className = "reading";
      this.speechSpeaking = true;
      window.speechSynthesis.speak(this.utterance);
    },
    stopSpeech() {
      try {
        this.skipAutoNext = true;
        window.speechSynthesis.cancel();
        const current = this.getCurrentParagraph();
        if (current) {
          current.className = "";
        }
      } catch (error) {
        //
      }
    },
    restartSpeech() {
      this.stopSpeech();
      setTimeout(() => {
        this.startSpeech();
      }, 100);
    },
    toggleSpeech() {
      this.speechSpeaking ? this.stopSpeech() : this.startSpeech();
    },
    speechPrev() {
      if (window.speechSynthesis.speaking) {
        this.stopSpeech();
      }
      const current = this.getCurrentParagraph();
      const prev = this.getPrevParagraph();
      if (prev) {
        this.showParagraph(prev, true);
        current.className = "";
        prev.className = "reading";
        this.startSpeech();
      } else {
        // 上一章
        this.$once("showContent", () => {
          setTimeout(() => {
            this.startSpeech();
          }, 100);
        });
        this.toLastChapter();
      }
    },
    speechNext() {
      if (window.speechSynthesis.speaking) {
        this.stopSpeech();
      }
      const current = this.getCurrentParagraph();
      const next = this.getNextParagraph();
      if (next) {
        this.showParagraph(next, true);
        current.className = "";
        next.className = "reading";
        this.startSpeech();
      } else {
        // 下一章
        this.$once("showContent", () => {
          setTimeout(() => {
            this.startSpeech();
          }, 100);
        });
        this.toNextChapter();
      }
    },
    getFirstVisibleReadingParagraph() {
      if (!this.$refs.bookContentRef || !this.$refs.bookContentRef.$el) {
        return null;
      }
      const list = this.$refs.bookContentRef.$el.querySelectorAll(
        "h3[data-pos],p[data-pos]"
      );
      const viewportTop = this.getReadingViewportTop();
      for (let i = 0; i < list.length; i++) {
        const position = list[i].getBoundingClientRect();
        if (this.isSlideRead) {
          if (position.right > 0 && position.left < this.windowSize.width) {
            return list[i];
          }
        } else if (
          position.bottom > viewportTop &&
          position.top < this.windowSize.height
        ) {
          return list[i];
        }
      }
      return null;
    },
    getCurrentParagraph() {
      const readingEle = this.$refs.bookContentRef.$el.querySelectorAll(
        ".reading"
      );
      let currentParagraph = null;
      if (!readingEle.length) {
        // 没有正在读的段落，遍历找到当前页面的第一段
        const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
        for (let i = 0; i < list.length; i++) {
          const elePos = list[i].getBoundingClientRect();
          if (this.isSlideRead) {
            // 段尾出现在视野里
            if (elePos.right > 0) {
              currentParagraph = list[i];
              break;
            }
          } else {
            // 段尾出现在视野里
            if (
              elePos.bottom >
              30 +
                20 +
                (window.webAppDistance | 0) +
                (this.$store.state.safeArea.top | 0)
            ) {
              currentParagraph = list[i];
              break;
            }
          }
        }
      } else {
        currentParagraph = readingEle[0];
      }
      return currentParagraph;
    },
    getPrevParagraph() {
      const current = this.getCurrentParagraph();
      const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
      for (let i = 0; i < list.length; i++) {
        if (i > 0 && current === list[i]) {
          return list[i - 1];
        }
      }
      return null;
    },
    getNextParagraph() {
      const current = this.getCurrentParagraph();
      const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
      for (let i = 0; i < list.length; i++) {
        if (current === list[i]) {
          return list[i + 1];
        }
      }
      return null;
    },
    exitRead() {
      this.stopSpeech();
      const current = this.getCurrentParagraph();
      this.showReadBar = false;
      this.showParagraph(current);
    },
    showParagraph(paragraph, scroll, callback) {
      if (!paragraph) {
        callback && callback();
        return;
      }
      if (this.isSlideRead) {
        // 跳转位置
        this.$nextTick(() => {
          const pos = paragraph.getBoundingClientRect();
          if (pos.left > this.windowSize.width - 16) {
            this.showPage(
              Math.round(pos.left / (this.windowSize.width - 16)) + 1,
              0
            );
          }
          callback && callback();
        });
      } else if (scroll) {
        // 跳转位置
        this.$nextTick(() => {
          const pos = paragraph.getBoundingClientRect();
          this.scrollContent(
            pos.top -
              (this.$store.state.miniInterface
                ? this.getFirstParagraphPos().bottom
                : 0) -
              (window.webAppDistance | 0) -
              (this.$store.state.safeArea.top | 0),
            0
          );
          callback && callback();
        });
      } else {
        callback && callback();
      }
    },
    getFirstParagraphPos() {
      return this.$refs.top.getBoundingClientRect();
    },
    scrollHandler() {
      if (this.selectionToolbarVisible) {
        this.hideSelectionToolbar(false);
      }
      const scrollTop =
        document.documentElement.scrollTop || document.body.scrollTop;
      if (!this.isSlideRead) {
        this.currentPage = Math.round(
          (scrollTop + this.windowSize.height) /
            (this.windowSize.height - this.scrollOffset)
        );
      }
      if (this.isScrollRead) {
        const lastScrollTop = this.lastScrollTop || 0;
        if (lastScrollTop > 0 && scrollTop == 0) {
          // 往上滚动到顶
          // if (!this.preCaching) {
          //   this.preCaching = true;
          //   const prevIndex = this.showChapterList[0].index - 1;
          //   if (prevIndex > 0) {
          //     this.showPrevChapterSize = this.chapterIndex - prevIndex;
          //     this.loadShowChapter(prevIndex).then(() => {
          //       setTimeout(() => {
          //         this.preCaching = false;
          //       }, 3000);
          //     });
          //   }
          // }
        } else if (
          scrollTop >
          document.documentElement.scrollHeight - 2 * this.windowSize.height // 倒数第三页
        ) {
          // 往下滚动到 倒数第三页
          if (!this.preCaching && this.startSavePosition) {
            this.preCaching = true;
            let nextIndex = this.chapterIndex + 1;
            if (this.showChapterList.length) {
              nextIndex =
                this.showChapterList[this.showChapterList.length - 1].index + 1;
            }
            this.showNextChapterSize = nextIndex - this.chapterIndex;
            // console.log("到底部了，加载下一章");
            this.loadShowChapter(nextIndex)
              .then(() => {
                this.computeShowChapterList();
                this.preCaching = false;
              })
              .catch(() => {
                this.preCaching = false;
              });
          }
        }
      }
      this.lastScrollTop = scrollTop;
      if (this.startSavePosition && !this.readingProgressMaxTimer) {
        this.readingProgressMaxTimer = setTimeout(() => {
          this.readingProgressMaxTimer = null;
          if (this.scrollTimer) {
            clearTimeout(this.scrollTimer);
            this.scrollTimer = null;
          }
          this.saveReadingPosition({ immediate: true });
        }, readingPositionMaxWait);
      }
      this.scrollTimer && clearTimeout(this.scrollTimer);
      this.scrollTimer = setTimeout(this.saveReadingPosition, 100);
    },
    beforeReadMethodChange() {
      this.currentParagraph = this.getCurrentParagraph();
    },
    getReadingViewportTop() {
      let viewportTop =
        (window.webAppDistance | 0) + (this.$store.state.safeArea.top | 0);
      if (this.$store.state.miniInterface && this.$refs.top) {
        viewportTop = Math.max(
          viewportTop,
          this.$refs.top.getBoundingClientRect().bottom
        );
      }
      return Math.max(0, viewportTop);
    },
    getVisualChapterElement(chapterIndex, findVisible) {
      const contentRef = this.$refs.bookContentRef;
      if (!contentRef || !contentRef.$el) {
        return null;
      }
      const root = contentRef.$el;
      const chapterElements = root.querySelectorAll(".chapter-content");
      if (chapterElements.length) {
        if (typeof chapterIndex !== "undefined" && chapterIndex !== null) {
          for (let i = 0; i < chapterElements.length; i++) {
            if (
              chapterElements[i].dataset &&
              Number(chapterElements[i].dataset.index) === Number(chapterIndex)
            ) {
              return chapterElements[i];
            }
          }
        }
        if (findVisible) {
          const viewportTop = this.getReadingViewportTop();
          for (let i = 0; i < chapterElements.length; i++) {
            if (
              chapterElements[i].getBoundingClientRect().bottom > viewportTop
            ) {
              return chapterElements[i];
            }
          }
        }
        return chapterElements[0];
      }
      return root;
    },
    getVisualReadingPosition(chapterElement) {
      const contentRef = this.$refs.bookContentRef;
      if (!contentRef || !contentRef.$el) {
        return 0;
      }
      if (this.isSlideRead && this.totalPages > 1) {
        return Math.round(
          ((Math.max(1, this.currentPage) - 1) /
            Math.max(1, this.totalPages - 1)) *
            readingPositionRatioScale
        );
      }
      const rect = (chapterElement || contentRef.$el).getBoundingClientRect();
      const viewportTop = this.getReadingViewportTop();
      const viewportHeight = Math.max(
        1,
        this.windowSize.height -
          viewportTop -
          (this.$store.state.safeArea.bottom | 0)
      );
      const maxTravel = Math.max(0, rect.height - viewportHeight);
      if (!maxTravel) {
        return 0;
      }
      const ratio = Math.max(
        0,
        Math.min(1, (viewportTop - rect.top) / maxTravel)
      );
      return Math.round(ratio * readingPositionRatioScale);
    },
    captureReadingProgress() {
      const book = this.$store.getters.readingBook || {};
      if (!book.bookUrl) {
        return null;
      }
      let chapterIndex = this.chapterIndex;
      let position = 0;
      let positionType = readingPositionTypes.text;
      if (this.isAudio) {
        positionType = readingPositionTypes.audio;
        position = this.$refs.bookContentRef
          ? Math.max(0, this.$refs.bookContentRef.currentTime | 0)
          : 0;
      } else if (
        this.isEpub ||
        this.isCarToon ||
        this.isCbz ||
        this.isEpubBook
      ) {
        positionType = readingPositionTypes.ratio;
        const chapterElement = this.getVisualChapterElement(undefined, true);
        if (
          chapterElement &&
          chapterElement.dataset &&
          typeof chapterElement.dataset.index !== "undefined"
        ) {
          const activeChapterIndex = Number(chapterElement.dataset.index);
          if (Number.isFinite(activeChapterIndex) && activeChapterIndex >= 0) {
            chapterIndex = Math.floor(activeChapterIndex);
          }
        }
        position = this.getVisualReadingPosition(chapterElement);
      } else {
        if (!this.$refs.bookContentRef) {
          return null;
        }
        this.currentParagraph = this.getFirstVisibleReadingParagraph();
        if (this.currentParagraph) {
          const paragraphPosition = Number(
            this.currentParagraph.dataset && this.currentParagraph.dataset.pos
          );
          if (Number.isFinite(paragraphPosition) && paragraphPosition >= 0) {
            position = Math.floor(paragraphPosition);
          }
          let currentChapter = this.currentParagraph;
          const contentRoot = this.$refs.bookContentRef.$el;
          while (
            currentChapter &&
            currentChapter !== contentRoot &&
            (!currentChapter.classList ||
              !currentChapter.classList.contains("chapter-content"))
          ) {
            currentChapter = currentChapter.parentNode;
          }
          if (
            currentChapter &&
            currentChapter.dataset &&
            typeof currentChapter.dataset.index !== "undefined"
          ) {
            const currentChapterIndex = Number(currentChapter.dataset.index);
            if (
              Number.isFinite(currentChapterIndex) &&
              currentChapterIndex >= 0
            ) {
              chapterIndex = Math.floor(currentChapterIndex);
            }
          }
        }
      }
      const chapter = (book.catalog || [])[chapterIndex] || {};
      return {
        bookUrl: book.bookUrl,
        userName: this.$store.getters.currentUserName || "default",
        chapterIndex,
        chapterTitle: chapter.title || this.title || "",
        position: Math.max(0, Math.floor(position)),
        positionType,
        updatedAt: Date.now(),
        pending: this.isBookInShelf(book.bookUrl),
        legacy: false,
        confirmed: false
      };
    },
    showVisualReadingPosition(progress, callback) {
      const applyPosition = () => {
        if (this._inactive || this._isDestroyed) {
          callback && callback();
          return;
        }
        if (
          progress.bookUrl !==
            (this.$store.getters.readingBook || {}).bookUrl ||
          progress.chapterIndex !== this.chapterIndex
        ) {
          callback && callback();
          return;
        }
        if (!this.$refs.bookContentRef || !this.$refs.bookContentRef.$el) {
          setTimeout(applyPosition, 10);
          return;
        }
        this.computePages(() => {
          const ratio = Math.max(
            0,
            Math.min(1, progress.position / readingPositionRatioScale)
          );
          if (this.isSlideRead && this.totalPages > 1) {
            const page =
              Math.round(ratio * Math.max(1, this.totalPages - 1)) + 1;
            this.showPage(page, 0);
          } else {
            const element =
              this.getVisualChapterElement(progress.chapterIndex, false) ||
              this.$refs.bookContentRef.$el;
            const rect = element.getBoundingClientRect();
            const currentScrollTop =
              document.documentElement.scrollTop || document.body.scrollTop;
            const viewportTop = this.getReadingViewportTop();
            const viewportHeight = Math.max(
              1,
              this.windowSize.height -
                viewportTop -
                (this.$store.state.safeArea.bottom | 0)
            );
            const maxTravel = Math.max(0, rect.height - viewportHeight);
            const elementTop = currentScrollTop + rect.top;
            const targetScrollTop =
              elementTop + ratio * maxTravel - viewportTop;
            this.scrollContent(targetScrollTop, 0, true);
          }
          callback && callback();
        });
      };
      this.$nextTick(applyPosition);
    },
    // 只会在进入的时候调用
    showPosition(progress, callback) {
      if (this._inactive || this._isDestroyed) {
        callback && callback();
        return;
      }
      progress = this.normalizeReadingProgress(
        progress,
        this.$store.getters.readingBook
      );
      if (!progress) {
        callback && callback();
        return;
      }
      let positionType = progress.positionType;
      if (!positionType && progress.legacy) {
        positionType = this.isAudio
          ? readingPositionTypes.audio
          : this.isEpub || this.isCarToon || this.isCbz || this.isEpubBook
          ? "legacyPixels"
          : readingPositionTypes.text;
      }
      if (!positionType) {
        positionType =
          this.isAudio ||
          this.isEpub ||
          this.isCarToon ||
          this.isCbz ||
          this.isEpubBook
            ? null
            : readingPositionTypes.text;
      }
      const epubContentReady = !!(
        this.$refs.bookContentRef && this.$refs.bookContentRef.iframeLoaded
      );
      if (positionType === readingPositionTypes.audio) {
        if (!this.$refs.bookContentRef) {
          setTimeout(() => this.showPosition(progress, callback), 10);
          return;
        }
        this.$refs.bookContentRef.ensureSeekTime(progress.position, callback);
      } else if (positionType === readingPositionTypes.ratio) {
        this.visualRestoreUntil = Date.now() + 2000;
        if (this.isEpub && !epubContentReady) {
          this.$once("iframeLoad", () => {
            this.showVisualReadingPosition(progress, callback);
          });
          this.showVisualReadingPosition(progress);
        } else if (this.isEpub) {
          this.showVisualReadingPosition(progress, callback);
        } else {
          this.showVisualReadingPosition(progress);
          setTimeout(() => {
            this.showVisualReadingPosition(progress, callback);
          }, 500);
        }
      } else if (positionType === "legacyPixels") {
        this.scrollContent(progress.position, 0, true);
        if (this.isEpub && !epubContentReady) {
          this.$once("iframeLoad", () => {
            this.scrollContent(progress.position, 0, true);
            callback && callback();
          });
        } else if (this.isEpub) {
          callback && callback();
        } else {
          setTimeout(() => {
            this.scrollContent(progress.position, 0, true);
            callback && callback();
          }, 500);
        }
      } else if (positionType === readingPositionTypes.text) {
        if (!this.$refs.bookContentRef) {
          setTimeout(() => this.showPosition(progress, callback), 10);
          return;
        }
        const list = this.$refs.bookContentRef.$el.querySelectorAll(
          ".reading-chapter h3, .reading-chapter p"
        );
        let paragraph = null;
        for (let i = 0; i < list.length; i++) {
          if (
            list[i].dataset &&
            typeof list[i].dataset.pos !== "undefined" &&
            +list[i].dataset.pos >= progress.position
          ) {
            paragraph = list[i];
            break;
          }
        }
        if (!paragraph && list.length) {
          paragraph = list[list.length - 1];
        }
        if (paragraph) {
          this.showParagraph(paragraph, true, callback);
        } else {
          callback && callback();
        }
      } else {
        if (this.isAudio && this.$refs.bookContentRef) {
          this.$refs.bookContentRef.ensureSeekTime(0, callback);
          return;
        } else {
          this.toTop(0, callback);
          return;
        }
      }
    },
    saveReadingPosition(options) {
      options =
        options && typeof options === "object" && !options.type ? options : {};
      try {
        if (this.error || !this.startSavePosition) {
          return null;
        }
        const progress = this.captureReadingProgress();
        if (!progress) {
          return null;
        }
        const cached = this.getCachedReadingProgress({
          ...this.$store.getters.readingBook,
          bookUrl: progress.bookUrl
        });
        const isShelfBook = this.isBookInShelf(progress.bookUrl);
        const hasProgressChange =
          !cached ||
          !this.isReadingProgressEqual(cached, progress) ||
          cached.pending ||
          cached.legacy;
        const chapterChanged = progress.chapterIndex !== this.chapterIndex;
        if (!hasProgressChange) {
          return progress;
        }
        progress.pending = isShelfBook;
        this.setCachedReadingProgress(
          progress,
          isShelfBook,
          progress.updatedAt
        );
        this.restoreReadingProgress = progress;
        if (chapterChanged) {
          this.$store.commit("setReadingBookIndex", progress);
          this.title = progress.chapterTitle || this.title;
        }
        if (!isShelfBook) {
          return progress;
        }
        this.enqueueReadingProgress(progress);
        if (options.beacon) {
          this.clearReadingProgressTimers();
          this.sendReadingProgressBeacon(progress);
        } else {
          if (options.immediate) {
            this.flushReadingProgress(true);
          } else {
            this.scheduleReadingProgressSync(progress);
          }
        }
        return progress;
      } catch (error) {
        return null;
      }
    },
    autoShowPosition(immediate) {
      const scheduledInitId = this.readingProgressInitId;
      const handler = () => {
        if (
          scheduledInitId !== this.readingProgressInitId ||
          this.error ||
          this._inactive ||
          (!immediate && this.isScrollRead)
        ) {
          return;
        }
        const book = this.$store.getters.readingBook || {};
        const restoreBookUrl = book.bookUrl;
        const restoreChapterIndex = this.chapterIndex;
        const restoreInitId = scheduledInitId;
        let progress = this.restoreReadingProgress;
        if (
          !progress ||
          progress.bookUrl !== book.bookUrl ||
          progress.chapterIndex !== this.chapterIndex
        ) {
          progress = this.selectReadingProgress(
            this.getServerReadingProgress(book),
            this.getCachedReadingProgress(book)
          );
        }
        let finished = false;
        const finishRestore = () => {
          if (
            finished ||
            restoreInitId !== this.readingProgressInitId ||
            restoreBookUrl !==
              (this.$store.getters.readingBook || {}).bookUrl ||
            restoreChapterIndex !== this.chapterIndex
          ) {
            return;
          }
          finished = true;
          this.startSavePosition = true;
          if (progress && (progress.pending || progress.legacy)) {
            setTimeout(() => {
              const migratedProgress = this.saveReadingPosition({
                immediate: true
              });
              if (
                progress.legacy &&
                migratedProgress &&
                migratedProgress.positionType &&
                window.localStorage
              ) {
                window.localStorage.removeItem(
                  this.getLegacyReadingProgressCacheKey(book)
                );
              }
            }, 0);
          }
        };
        this.$nextTick(() => {
          if (progress) {
            this.showPosition(progress, finishRestore);
          } else {
            this.toTop(0, finishRestore);
          }
        });
      };
      if (immediate) {
        handler();
      } else {
        this.$once("showContent", handler);
      }
    },
    wakeLock() {
      if ("WakeLock" in window && "request" in window.WakeLock) {
        let wakeLock = null;
        const requestWakeLock = () => {
          const controller = new AbortController();
          const signal = controller.signal;
          window.WakeLock.request("screen", { signal }).catch(e => {
            if (e.name === "AbortError") {
              // console.log("Wake Lock was aborted");
            } else {
              // console.error(`${e.name}, ${e.message}`);
            }
          });
          // console.log("Wake Lock is active");
          return controller;
        };

        wakeLock = requestWakeLock();

        const handleVisibilityChange = () => {
          if (wakeLock !== null && document.visibilityState === "visible") {
            wakeLock = requestWakeLock();
          }
        };

        document.addEventListener("visibilitychange", handleVisibilityChange);
        document.addEventListener("fullscreenchange", handleVisibilityChange);
        return () => {
          if (wakeLock != null) {
            wakeLock.abort();
            wakeLock = null;
          }
          document.removeEventListener(
            "visibilitychange",
            handleVisibilityChange
          );
          document.removeEventListener(
            "fullscreenchange",
            handleVisibilityChange
          );
        };
      } else if ("wakeLock" in navigator && "request" in navigator.wakeLock) {
        let wakeLock = null;
        const requestWakeLock = async () => {
          try {
            wakeLock = await navigator.wakeLock.request("screen");
            wakeLock.addEventListener("release", () => {
              // console.log("Wake Lock was released");
            });
            // console.log("Wake Lock is active");
          } catch (e) {
            // console.error(`${e.name}, ${e.message}`);
          }
        };
        requestWakeLock();
        const handleVisibilityChange = () => {
          if (wakeLock !== null && document.visibilityState === "visible") {
            requestWakeLock();
          }
        };
        document.addEventListener("visibilitychange", handleVisibilityChange);
        document.addEventListener("fullscreenchange", handleVisibilityChange);
        return () => {
          if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
          }
          document.removeEventListener(
            "visibilitychange",
            handleVisibilityChange
          );
          document.removeEventListener(
            "fullscreenchange",
            handleVisibilityChange
          );
        };
      }
    },
    lazyloadHandler() {
      if (!this.isAudio) {
        this.computePages(() => {
          const progress = this.restoreReadingProgress;
          if (
            progress &&
            progress.positionType === readingPositionTypes.ratio &&
            Date.now() < this.visualRestoreUntil &&
            progress.bookUrl === this.readingBook.bookUrl &&
            progress.chapterIndex === this.chapterIndex
          ) {
            this.showVisualReadingPosition(progress);
          }
        });
      }
    },
    showCacheContent() {
      this.showCacheContentZone = !this.showCacheContentZone;
    },
    cacheChapterContent(cacheCount) {
      //
      let cacheChapterList = [];
      if (cacheCount === true) {
        //
        cacheChapterList = cacheChapterList.concat(
          this.catalog.slice(this.chapterIndex + 1, this.catalog.length)
        );
      } else {
        //
        cacheChapterList = cacheChapterList.concat(
          this.catalog.slice(
            this.chapterIndex + 1,
            Math.min(this.catalog.length, this.chapterIndex + 1 + cacheCount)
          )
        );
      }
      if (!cacheChapterList.length) {
        this.$message.error("不需要缓存");
        return;
      }
      this.isCachingContent = true;
      this.cachingContentTip = "正在缓存章节  0/" + cacheChapterList.length;
      this.cachingHandler = LimitResquest(2, handler => {
        this.cachingContentTip =
          "正在缓存章节  " +
          handler.requestCount +
          "/" +
          cacheChapterList.length;
        if (handler.isEnd()) {
          this.$message.success("缓存完成");
          this.isCachingContent = false;
          this.cachingContentTip = "";
        }
      });
      cacheChapterList.forEach(v => {
        this.cachingHandler(() => {
          return this.getBookContent(
            v.index,
            {
              timeout: 30000,
              silent: true
            },
            false,
            true
          );
        });
      });
    },
    cancelCaching() {
      if (this.cachingHandler && this.cachingHandler.cancel) {
        this.cachingHandler.cancel();
        this.isCachingContent = false;
        this.cachingContentTip = "";
      }
    },
    startAutoReading() {
      this.showToolBar = false;
      this.autoReading = true;
      this.autoRead();
    },
    autoRead() {
      if (!this.autoReading) {
        return;
      }
      if (this.showToolBar) {
        this.autoReadingTimer = setTimeout(() => {
          this.autoRead();
        }, 300);
        return;
      }
      if (this.config.autoReadingMethod === "像素滚动") {
        this.autoReadByPixel();
        return;
      }
      const current = this.getCurrentParagraph();
      const next = this.getNextParagraph();
      if (next) {
        current.className = "reading";
        next.className = "";
        // 计算当前段落
        let delayTime = this.config.autoReadingLineTime;
        try {
          const currentPos = current.getBoundingClientRect();
          delayTime =
            delayTime *
            Math.ceil(
              currentPos.height / this.config.fontSize / this.config.lineHeight
            );
        } catch (error) {
          //
        }
        // console.log(delayTime, next);
        this.autoReadingTimer = setTimeout(() => {
          current.className = "";
          next.className = "reading";
          this.showParagraph(next, true);

          setTimeout(() => {
            this.autoRead();
          }, 32);
        }, delayTime);
      } else {
        // 下一章
        this.$once("showContent", () => {
          setTimeout(() => {
            this.autoRead();
          }, 100);
        });
        this.toNextChapter(() => {
          this.autoReading = false;
        });
      }
    },
    autoReadByPixel() {
      if (!this.autoReading) {
        return;
      }
      if (this.showToolBar) {
        this.autoReadingTimer = setTimeout(() => {
          this.autoRead();
        }, 300);
        return;
      }
      if (this.config.autoReadingMethod !== "像素滚动") {
        this.autoRead();
        return;
      }
      const scrollTop =
        document.documentElement.scrollTop || document.body.scrollTop;
      if (
        scrollTop + this.windowSize.height <
        document.documentElement.scrollHeight
      ) {
        // console.log(delayTime, next);
        this.autoReadingTimer = setTimeout(() => {
          // 滚动
          this.scrollContent(this.config.autoReadingPixel, 0);
          this.autoReadByPixel();
        }, this.config.autoReadingLineTime);
      } else {
        // 下一章
        this.$once("showContent", () => {
          setTimeout(() => {
            this.autoReadByPixel();
          }, 100);
        });
        this.toNextChapter(() => {
          this.autoReading = false;
        });
      }
    },
    stopAutoReading() {
      if (this.autoReadingTimer) {
        clearInterval(this.autoReadingTimer);
      }
      this.autoReading = false;
      const current = this.getCurrentParagraph();
      current.className = "";
    },
    toggleAutoReading() {
      if (this.autoReading) {
        this.stopAutoReading();
      } else {
        this.startAutoReading();
      }
    },
    showReadingBookInfo() {
      let book = { ...this.$store.getters.readingBook };
      const shelfBook = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      book = Object.assign(book, shelfBook || {});
      eventBus.$emit("showBookInfoDialog", book);
    },
    formatChinese(text) {
      if (this.isEpub || this.isAudio || this.isCbz || this.isCarToon) {
        return text;
      }
      if (this.config.chineseFont === "简体") {
        return simplized(text);
      } else {
        return traditionalized(text);
      }
    },
    showSearchBookContentDialog(keyword) {
      let book = { ...this.$store.getters.readingBook };
      const shelfBook = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      book = Object.assign(book, shelfBook || {});
      eventBus.$emit("showSearchBookContentDialog", book, keyword || "");
    },
    showMatchKeyword(data) {
      if (this._inactive) {
        return;
      }
      if (!this.$refs.bookContentRef) {
        setTimeout(() => {
          this.showMatchKeyword(data);
        }, 10);
        return;
      }
      try {
        const list = this.$refs.bookContentRef.$el.querySelectorAll(
          ".reading-chapter h3, .reading-chapter p"
        );
        let matchCount = 0;
        for (let i = 0; i < list.length; i++) {
          const pContent = list[i].innerText;
          let startIndex = -1;
          let isFound = false;
          // eslint-disable-next-line no-constant-condition
          while (true) {
            startIndex = pContent.indexOf(data.query, startIndex + 1);
            if (startIndex >= 0) {
              matchCount++;
              if (matchCount === data.resultCountWithinChapter + 1) {
                isFound = true;
                this.showParagraph(list[i], true);
                break;
              }
            } else {
              break;
            }
          }
          if (isFound) {
            break;
          }
        }
      } catch (error) {
        // console.error(error);
      }
    },
    getParagraphListInView() {
      // 获取视口内的所有段落
      const list = this.$refs.bookContentRef.$el.querySelectorAll("h3,p");
      const paragraphList = [];
      for (let i = 0; i < list.length; i++) {
        const elePos = list[i].getBoundingClientRect();
        if (this.isSlideRead) {
          // 段尾出现在视野里
          if (elePos.right > 0 && elePos.left > 0) {
            paragraphList.push(list[i]);
          }
        } else {
          // 段尾出现在视野里
          if (
            elePos.bottom >
              30 +
                20 +
                (window.webAppDistance | 0) +
                (this.$store.state.safeArea.top | 0) &&
            elePos.bottom < this.windowSize.height
          ) {
            paragraphList.push(list[i]);
          }
        }
      }
      return paragraphList;
    },
    showBookmarkDialog() {
      let book = { ...this.$store.getters.readingBook };
      const shelfBook = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      book = Object.assign(book, shelfBook || {});
      eventBus.$emit("showBookmarkDialog", book);
    },
    getContentMatchParagraph(text, distance, minDistance) {
      distance = distance || 0.7;
      // 正则过滤标点符号后，近似匹配每一段内容
      let paragraphList = text
        .replace(/\\n+/g, "\n")
        .split(/\n+/)
        .map(v => v.replace(symboRegex, ""))
        .filter(v => v);
      try {
        const list = this.$refs.bookContentRef.$el.querySelectorAll(
          ".reading-chapter h3, .reading-chapter p"
        );
        let paragraph = null;
        for (let i = 0; i < list.length; i++) {
          let isMatch = true;
          let pos = 0;
          let startPos = i;
          for (let j = 0; j < paragraphList.length; j++) {
            // 过滤所有字符
            let content = null;
            while (i + pos < list.length) {
              content = list[i + pos].innerText.replace(symboRegex, "");
              if (!content.length) {
                pos++;
                startPos++;
              } else {
                break;
              }
            }
            if (!content) {
              // 说明没找到有内容的段落，终止匹配
              isMatch = false;
              break;
            }
            const paragraphDistance = editDistance(content, paragraphList[j]);
            if (paragraphDistance < distance) {
              isMatch = false;
              break;
            } else {
              pos++;
            }
          }
          if (isMatch) {
            paragraph = list[startPos];
            break;
          }
        }
        if (paragraph) {
          return paragraph;
        }
        if (distance - 0.1 >= minDistance) {
          return this.getContentMatchParagraph(
            text,
            distance - 0.1,
            minDistance
          );
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
      return null;
    },
    showContentMatchParagraph(content) {
      if (this._inactive) {
        return;
      }
      const paragraph = this.getContentMatchParagraph(content, 1, 0.6);
      if (paragraph) {
        this.showParagraph(paragraph, true);
      } else {
        this.$message.error("无法定位内容所在段落");
      }
    },
    showBookmark(bookmark) {
      if (this._inactive) {
        return;
      }
      if (!this.$refs.bookContentRef) {
        setTimeout(() => {
          this.showBookmark(bookmark);
        }, 10);
        return;
      }
      this.showContentMatchParagraph(bookmark.bookText);
    }
  }
};
</script>

<style lang="stylus" scoped>
>>>.popper-component {
  margin-left: 10px;
}

.chapter-wrapper {
  padding: 0;
  flex-direction: column;
  align-items: center;

  >>>.no-point {
    pointer-events: none;
  }

  .tool-bar {
    position: fixed;
    top: 0;
    padding-top: 0;
    padding-top: constant(safe-area-inset-top) !important;
    padding-top: env(safe-area-inset-top) !important;
    left: 50%;
    z-index: 2001;
    border-radius: 0 0 var(--ui-radius) var(--ui-radius);
    overflow: hidden;
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        position: relative;
        font-size: 18px;
        width: 58px;
        height: 52px;
        text-align: center;
        padding-top: 10px;
        box-sizing: border-box;
        cursor: pointer;
        outline: none;
        transition: all var(--ui-transition);

        &:hover {
          transform: translateY(-1px);
        }

        &:active {
          transform: translateY(0);
        }

        .iconfont {
          font-family: iconfont;
          width: 16px;
          font-size: 16px;
          margin: 0 auto;
          height: 22px;
          line-height: 22px;
          vertical-align: middle;
        }

        .tool-el-icon {
          font-size: 18px;
          line-height: 22px;
          height: 22px;

          i {
            line-height: 22px;
          }
        }

        .icon-text {
          font-size: 12px;
        }
      }
    }
  }

  .read-bar {
    position: fixed;
    bottom: 0;
    right: 50%;
    z-index: 100;
    border-radius: var(--ui-radius) var(--ui-radius) 0 0;
    overflow: visible;
    backdrop-filter: blur(18px);
    -webkit-backdrop-filter: blur(18px);

    .progress {
      padding: 10px 36px;
      display: flex;
      justify-content: space-between;
      align-items: center;

      .progress-bar {
        flex: 1;
        padding: 0 10px;
      }

      .progress-tip {
        font-size: 14px;
        margin-left: 5px;
      }
    }

    .cache-content-zone {
      padding: 10px 36px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
      position: absolute;
      right: 55px;
      width: 300px;
      background: inherit;
      border-radius: 8px;

      .cache-content-btn {
        cursor: pointer;
        padding: 4px 6px;
        border-radius: 4px;
      }
    }

    .float-left-btn-zone {
      position: absolute;
      bottom: 155px;
      left: 4px;
      right: auto;
      display: flex;
      flex-direction: column;

      .float-btn {
        line-height: 32px;
        width: 38px;
        height: 38px;
        border-radius: var(--ui-radius-sm);
        display: block;
        cursor: pointer;
        text-align: center;
        vertical-align: middle;
        pointer-events: all;
        margin-top: 16px;
        transition: all var(--ui-transition);

        &:hover {
          transform: translateY(-2px);
        }

        &:active {
          transform: translateY(0);
        }

        .el-icon-top, .el-icon-bottom, .el-icon-info, .el-icon-search, .el-icon-collection-tag {
          line-height: 38px;
        }
      }
    }

    .float-right-btn-zone {
      position: absolute;
      bottom: 155px;
      left: 4px;
      right: auto;
      display: flex;
      flex-direction: column;

      .float-btn {
        line-height: 32px;
        width: 38px;
        height: 38px;
        border-radius: var(--ui-radius-sm);
        display: block;
        cursor: pointer;
        text-align: center;
        vertical-align: middle;
        pointer-events: all;
        margin-top: 16px;
        transition: all var(--ui-transition);

        &:hover {
          transform: translateY(-2px);
        }

        &:active {
          transform: translateY(0);
        }

        .el-icon-refresh-right, .el-icon-headset, .el-icon-view {
          line-height: 38px;
        }
        .el-icon-moon {
          color: var(--ui-text);
          line-height: 36px;
        }
        .el-icon-sunny {
          color: var(--ui-text-secondary);
          line-height: 36px;
        }
      }
    }

    .tools {
      display: flex;
      flex-direction: column;

      .tool-icon {
        font-size: 18px;
        width: 42px;
        height: 42px;
        padding-top: 8px;
        box-sizing: border-box;
        text-align: center;
        align-items: center;
        cursor: pointer;
        outline: none;
        margin-top: -1px;
        transition: background-color 0.18s ease, color 0.18s ease;

        &.progress-text {
          font-size: 13px;
          line-height: 1.25;
          padding: 7px 4px 0;
        }

        .iconfont {
          font-family: iconfont;
          width: 16px;
          height: 16px;
          font-size: 16px;
          margin: 0 auto 6px;
        }
      }
    }

    .reader-bar-inner {
      display: flex;
      flex-direction: column;
      padding-bottom: 10px;
      padding-bottom: calc(10px + constant(safe-area-inset-top));
      padding-bottom: calc(10px + env(safe-area-inset-top));
      padding-left: 5px;
      padding-right: 5px;

      .operate-bar {
        display: flex;
        flex-direction: row;
        justify-content: space-between;
        padding: 10px 10px 0 10px;
        align-items: center;

        .close-btn, .collapse-btn {
          font-size: 22px;
          height: 35px;
          cursor: pointer;
        }

        .center {
          span {
            display: inline-block;
            cursor: pointer;
          }
          .play-pause-btn {
            font-size: 50px;
            margin-top: -40px;
            i {
              border-radius: 100%;
            }
          }
          .ctrl-btn {
            margin: 0px 15px;
          }
        }
      }

      .setting-item {
        display: flex;
        flex-direction: column;
        padding: 5px 10px;

        .setting-title {
          font-size: 14px;
        }

        .setting-btn {
          font-size: 14px;
          cursor: pointer;
          display: inline-block;
          margin-left: 5px;
        }

        .voice-list {
          display: flex;
          flex-direction: row;
          overflow-x: auto;
          padding: 5px 10px;

          .radio-group {
            white-space: nowrap;

            .radio-button {
              margin-right: 10px;

              .el-radio-button__inner {
                border-radius: 4px 4px 4px 4px;
              }
            }
          }
        }

        .progress {
          padding: 5px 10px;

          .progress-tip {
            margin-left: 0;
            margin-right: 5px;
          }
        }
      }
    }
  }

  .chapter-bar {
    .el-breadcrumb {
      .item {
        font-size: 14px;
        color: #606266;
      }
    }
  }

  .chapter {
    font-family: 'Microsoft YaHei', PingFangSC-Regular, HelveticaNeue-Light, 'Helvetica Neue Light', sans-serif;
    text-align: left;
    padding: 0 65px;
    min-height: 100vh;
    min-height: calc(var(--vh, 1vh) * 100);
    width: 670px;
    margin: 0 auto;
    background-size: cover;
    position: relative;

    >>>.el-icon-loading {
      font-size: 36px;
      color: var(--ui-text-muted);
    }

    >>>.el-loading-text {
      font-weight: 500;
      color: var(--ui-text-muted);
    }

    .click-zone {
      position: absolute;
      z-index: 120;
      top: 0;
      bottom: 0;
      left: 0;
      right: 0;
      background: #333;
      opacity: 0.8;
      color: #fff;
      font-size: 14px;
      pointer-events: none;

      div {
        position: absolute;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .close-btn {
        left: 0;
        right: 0;
        bottom: 20px;
        height: 45px;
        line-height: 45px;
        z-index: 10;
        padding: 0;
        cursor: pointer;
        pointer-events: all;
      }
    }

    .content {
      font-size: 18px;
      line-height: 1.8;
      overflow: hidden;
      font-family: 'Microsoft YaHei', PingFangSC-Regular, HelveticaNeue-Light, 'Helvetica Neue Light', sans-serif;

      .content-inner {
        min-height: calc(var(--vh, 1vh) * 80);
        padding-bottom: 25px;
        box-sizing: border-box;
      }
    }

    .bottom-bar, .top-bar {
      box-sizing: border-box;
    }
    .top-bar {
      height: 44px;
      padding: 10px;
    }
    .bottom-bar {
      width: 100%;
      text-align: center;
      padding-bottom: 30px;
      .bottom-btn {
        font-size: 14px;
        cursor: pointer;
        display: inline-block;
        margin: 0 auto;
        padding: 10px 40px;
        width: 80%;
        box-sizing: border-box;
      }
    }
  }

  .chapter.audio {
    .top-bar, .bottom-bar {
      display: none;
    }
    .content-inner {
      height: calc(var(--vh, 1vh) * 100);
      margin-top: 0 !important;
      padding-top: 0 !important;
      padding-bottom: 0 !important;
      display: flex;
      align-items: center;
    }
  }
}

.day {
  >>>.popup {
    box-shadow: var(--ui-shadow);
  }

  >>>.tool-icon {
    border: 1px solid var(--ui-border);
    margin-top: -1px;
    color: var(--ui-text);

    .icon-text {
      color: var(--ui-text-secondary);
    }
  }

  >>>.tool-icon:hover {
    background: rgba(79,110,247,.06);
    color: var(--ui-text);
  }

  >>>.progress-tip {
    color: var(--ui-text-secondary);
  }

  >>>.cache-content-zone {
    color: var(--ui-text-secondary);
    box-shadow: var(--ui-shadow);

    .cache-content-btn:hover {
      background: rgba(79,110,247,.06);
      color: var(--ui-text);
    }
  }

  >>>.float-left-btn-zone {
    color: var(--ui-text);
  }

  >>>.float-right-btn-zone {
    color: var(--ui-text);
  }

  >>>.float-btn {
    box-shadow: var(--ui-shadow);
  }

  >>>.float-btn:hover {
    background: rgba(79,110,247,.06) !important;
    box-shadow: var(--ui-shadow-lg);
  }

  >>>.reader-bar-inner {
    color: var(--ui-text);

    .setting-title {
      color: var(--ui-text);
    }

    .setting-value {
      color: var(--ui-text-secondary);
    }
  }

  >>>.chapter {
    border: 1px solid var(--ui-border);
    color: var(--ui-text);
  }

  .bottom-bar, .top-bar {
    color: var(--ui-text-secondary);
  }

  >>>.el-slider__runway {
    background-color: #fff;
  }

  >>>.play-pause-btn {
    color: var(--ui-accent);
  }
}

.night {
  >>>.popup {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
  }

  >>>.tool-icon {
    border: 1px solid rgba(255,255,255,.08);
    margin-top: -1px;
    color: var(--ui-text-secondary);

    .icon-text {
      color: var(--ui-text-muted);
    }
  }

  >>>.tool-icon:hover {
    background: rgba(107,138,255,.1);
    color: var(--ui-text);
  }

  >>>.progress-tip {
    color: var(--ui-text-muted);
  }

  >>>.cache-content-zone {
    color: var(--ui-text-muted);
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35);

    .cache-content-btn:hover {
      background: rgba(107,138,255,.1);
      color: var(--ui-text-secondary);
    }
  }

  >>>.float-left-btn-zone {
    color: var(--ui-text-secondary);
  }

  >>>.float-right-btn-zone {
    color: var(--ui-text-secondary);
  }

  >>>.float-btn {
    box-shadow: 0 8px 22px rgba(0, 0, 0, 0.4);
  }

  >>>.float-btn:hover {
    background: rgba(107,138,255,.1) !important;
    box-shadow: 0 10px 26px rgba(0, 0, 0, 0.5);
  }

  >>>.reader-bar-inner {
    color: var(--ui-text-secondary);
  }

  >>>.chapter {
    border: 1px solid rgba(255,255,255,.06);
    color: var(--ui-text-secondary);
  }

  >>>.popper__arrow {
    background: var(--ui-text-muted);
  }

  .bottom-bar, .top-bar {
    color: var(--ui-text-muted);
  }

  >>>.el-slider__runway {
    background-color: #2a2b32;
  }
  >>>.el-slider__bar {
    background-color: var(--ui-accent);
  }
  >>>.el-slider__button {
    border: 2px solid var(--ui-accent);
    background-color: #2a2b32;
  }
  >>>.play-pause-btn {
    color: var(--ui-accent);
  }
}

.chapter-wrapper {
  .read-bar {
    .float-btn-zone {
      position: absolute;
      bottom: 135px;
      left: 4px;

      .float-left-btn-zone {
        position: relative;
        left: auto;
        bottom: auto;
      }

      .float-right-btn-zone {
        position: relative;
        left: auto;
        bottom: auto;
        margin-bottom: 20px;
      }
    }

  }
}

.chapter-wrapper.mini-interface {
  padding: 0;
  position: relative;
  height: 100%;

  .tool-bar {
    left: 0;
    width: 100vw;
    margin-left: 0 !important;
    border-radius: 0;

    .tools {
      flex-direction: row;
      justify-content: space-around;
      .tool-icon {
        border: none;
      }
    }
  }

  .read-bar {
    right: 0;
    width: 100vw;
    margin-right: 0 !important;
    border-radius: 0;

    .cache-content-zone {
      position: relative;
      width: auto;
      right: 0;
      background: inherit;
    }

    .float-btn-zone {
      position: static;
      bottom: 0;
      left: 0;
    }

    .float-left-btn-zone {
      position: absolute;
      right: auto;
      left: 20px;
      bottom: 135px;
    }

    .float-right-btn-zone {
      position: absolute;
      left: auto;
      right: 20px;
      bottom: 135px;
    }

    .tools {
      flex-direction: row;
      justify-content: space-around;
      padding: 0 15px;
      height: 45px;
      .tool-icon {
        border: none;
        width: auto;
        padding: 0 6px;
        height: 45px;
        line-height: 45px;
        .iconfont {
          display: inline-block;
        }
        span {
          vertical-align: middle;
        }
      }
    }
  }

  .chapter {
    width: 100vw !important;
    padding: 0 16px;
    box-sizing: border-box;
    border: none;
    text-align: justify;
    position: relative;

    .top-bar {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      z-index: 50;
      background: inherit;
      height: 30px;
      height: calc(30px + constant(safe-area-inset-top));
      height: calc(30px + env(safe-area-inset-top));
      padding: 6px 16px;
      padding-top: calc(6px + constant(safe-area-inset-top));
      padding-top: calc(6px + env(safe-area-inset-top));
      font-size: 12px;
    }

    .content-inner {
      margin-top: 30px;
      margin-top: calc(30px + constant(safe-area-inset-top));
      margin-top: calc(30px + env(safe-area-inset-top));
      padding-top: 15px;
      padding-bottom: 15px;
    }
  }

  .chapter.cartoon {
    padding: 0;

    .content-inner {
      padding-top: 1px;
    }
  }

  .chapter.slide-reader {
    padding: 0;
    height: 100%;

    .bottom-bar {
      height: 24px;
      position: absolute;
      bottom: 0;
      padding: 0 16px;
      padding-bottom: 6px;
      display: flex;
      justify-content: space-between;
      font-size: 12px;
    }

    .top-bar {
      position: relative;
    }

    .content {
      position: absolute;
      overflow: visible;
      top: 30px;
      top: calc(30px + constant(safe-area-inset-top));
      top: calc(30px + env(safe-area-inset-top));
      bottom: 24px;
    }

    .content-inner {
      margin: 0 16px;
      overflow: hidden;
      text-align: justify;
      padding: 0;
      height: 100%;
    }

    .book-content {
      height: 100%;
      -webkit-columns: calc(100vw - 32px) 1;
      -webkit-column-gap: 32px;
      columns: calc(100vw - 16px) 1;
      column-gap: 16px;
    }
  }
}
.chapter-wrapper.mini-interface::-webkit-scrollbar {
  width: 0 !important;
}
</style>
<style lang="stylus">
.voice-list {
  .el-radio-button__inner {
    border-radius: var(--ui-radius-sm) !important;
    border-left: 1px solid var(--ui-border);
    box-shadow: none;
  }
}
.selection-floating-toolbar {
  position: fixed;
  z-index: 3005;
  display: flex;
  align-items: center;
  max-width: calc(100vw - 16px);
  overflow-x: auto;
  gap: 4px;
  padding: 6px;
  border: 1px solid rgba(0,0,0,.08);
  border-radius: var(--ui-radius-sm);
  background: rgba(255,255,255,.96);
  box-shadow: var(--ui-shadow-lg);
  transform: translateX(-50%);
  transition: opacity var(--ui-transition);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  -webkit-overflow-scrolling: touch;

  &::-webkit-scrollbar {
    display: none;
  }

  .selection-toolbar-btn {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    height: 32px;
    padding: 0 9px;
    border: none;
    border-radius: var(--ui-radius-sm);
    background: transparent;
    color: var(--ui-text);
    font-size: 13px;
    line-height: 1;
    white-space: nowrap;
    cursor: pointer;
    outline: none;

    &:hover {
      background: rgba(79,110,247,.08);
      color: var(--ui-accent);
    }

    &.primary {
      background: var(--ui-accent);
      color: #fff;
    }

    &.primary:hover {
      background: var(--ui-accent-hover);
      color: #fff;
    }

    &.icon-only {
      width: 32px;
      padding: 0;
    }
  }
}
.dictionary-dialog {
  border-radius: var(--ui-radius);

  .el-dialog__body {
    padding-top: 12px;
    padding-bottom: 8px;
  }

  .dictionary-panel {
    min-height: 120px;
    color: var(--ui-text);
  }

  .dictionary-word-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .dictionary-word {
    flex: 1;
    min-width: 0;
    font-size: 22px;
    line-height: 1.35;
    font-weight: 600;
    word-break: break-word;
  }

  .dictionary-phonetics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 8px;
  }

  .dictionary-phonetic {
    font-size: 13px;
    line-height: 1.4;
    color: var(--ui-muted);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  }

  .dictionary-section {
    margin-top: 16px;
  }

  .dictionary-section-title {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--ui-muted);

    span {
      font-weight: 400;
    }
  }

  .dictionary-tip {
    margin-top: 8px;
    color: var(--ui-muted);
    font-size: 13px;
    line-height: 1.5;
  }

  .dictionary-meaning {
    padding: 10px 0;
    border-top: 1px solid var(--ui-border);

    &:first-of-type {
      border-top: none;
      padding-top: 0;
    }

    ol {
      margin: 6px 0 0 20px;
      padding: 0;
    }

    li {
      padding: 4px 0;
      line-height: 1.55;
      word-break: break-word;
    }
  }

  .dictionary-part {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 13px;
    color: var(--ui-muted);
  }

  .dictionary-type {
    display: inline-block;
    margin-right: 4px;
    color: var(--ui-muted);
  }

  .dictionary-example {
    margin-top: 4px;
    color: var(--ui-muted);
    font-size: 13px;
  }

  .dictionary-error {
    margin-top: 16px;
    color: #d9534f;
    line-height: 1.6;
  }
}
.night-theme {
  .dictionary-dialog {
    background: #24252b;
    color: #c5c8ce;

    .el-dialog__title,
    .dictionary-panel,
    .dictionary-word {
      color: #c5c8ce;
    }

    .dictionary-meaning {
      border-color: rgba(255,255,255,.1);
    }

    .dictionary-phonetic,
    .dictionary-section-title,
    .dictionary-tip,
    .dictionary-part,
    .dictionary-type,
    .dictionary-example {
      color: #909399;
    }
  }

  .selection-floating-toolbar {
    border-color: rgba(255,255,255,.1);
    background: rgba(36,37,43,.96);

    .selection-toolbar-btn {
      color: #c5c8ce;

      &:hover {
        background: rgba(79,110,247,.16);
        color: #fff;
      }
    }
  }

  .voice-list {
    .el-radio-button {
      box-shadow: none !important;
    }
    .el-radio-button__inner {
      background-color: #35363e;
      border-color: rgba(255,255,255,.1);
      color: #c5c8ce;
    }
    .el-radio-button__inner:hover {
      color: var(--ui-accent);
    }
    .el-radio-button__orig-radio:checked+.el-radio-button__inner {
      background-color: var(--ui-accent);
      border-color: var(--ui-accent);
      color: #fff;
      box-shadow: none;
    }
  }
}
.kindle-page {
  .day {
    .tool-icon {
      border: 1px solid #fefefefe;

      .icon-text {
        color: #444;
      }
    }

    .progress-tip {
      color: #444;
    }

    .cache-content-zone {
      color: #444;
    }

    .reader-bar-inner {

      .setting-title {
        color: rgba(0, 0, 0, 0.8);
      }

      .setting-value {
        color: #444;
      }
    }

    .bottom-bar, .top-bar {
      color: #444;
    }
  }
}
</style>
