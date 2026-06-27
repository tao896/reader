<template>
  <div id="app">
    <keep-alive>
      <router-view></router-view>
    </keep-alive>
    <el-dialog :visible.sync="showLogin" :width="dialogWidth" :top="dialogTop">
      <div class="custom-dialog-title" slot="title">
        <span class="el-dialog__title"
          >{{ isLogin ? "登录" : "注册" }}
          <span class="float-right span-btn" @click="isLogin = !isLogin">{{
            isLogin ? "注册" : "登录"
          }}</span>
        </span>
      </div>
      <el-form :model="loginForm">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" autocomplete="on"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            type="password"
            v-model="loginForm.password"
            autocomplete="on"
            show-password
            @keyup.enter.native="login"
          ></el-input>
        </el-form-item>
        <el-form-item label="邀请码(没有则不填)" v-if="!isLogin">
          <el-input
            v-model="loginForm.code"
            autocomplete="off"
            @keyup.enter.native="login"
          ></el-input>
        </el-form-item>
        <el-checkbox v-model="remember">记住登录信息</el-checkbox>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button size="medium" @click="cancel">取 消</el-button>
        <el-button size="medium" type="primary" @click="login">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog
      :title="editorTitle"
      :visible.sync="showEditor"
      :width="dialogWidth"
      :top="$store.state.miniInterface ? '0' : '10vh'"
      :fullscreen="$store.state.miniInterface"
    >
      <div class="code-editor language-json" ref="editorRef"></div>
      <div slot="footer" class="dialog-footer">
        <el-button size="medium" @click="closeEditor">取 消</el-button>
        <el-button size="medium" type="primary" @click="saveEditor"
          >保 存</el-button
        >
      </div>
    </el-dialog>

    <ImageViewer
      :z-index="3200"
      :initial-index="$store.state.previewImageIndex"
      v-if="$store.state.showImageViewer"
      :on-close="closeViewer"
      :url-list="$store.state.previewImgList"
    />

    <ReplaceRuleForm
      v-model="showReplaceRuleForm"
      :rule="replaceRule"
      :isAdd="isAddReplaceRule"
    />

    <ReplaceRule v-model="showReplaceRuleDialog" />

    <MPCode v-model="showMPCodeDialog" />

    <BookManage v-model="showBookManageDialog" />

    <BookInfo v-model="showBookInfoDialog" />

    <UserManage v-model="showUserManageDialog" />

    <AddUser v-model="showAddUserDialog" />

    <BookGroup v-model="showBookGroupDialog" :isSet="isSetBookGroup" />

    <RssSourceList v-model="showRssSourceListDialog" />
    <RssArticleList v-model="showRssArticleListDialog" :rssSource="rssSource" />
    <RssArticle
      v-model="showRssArticleDialog"
      :rssArticleInfo="rssArticleInfo"
    />

    <SearchBookContent
      v-model="showSearchBookContentDialog"
      :book="searchBook"
    />

    <BookmarkForm
      v-model="showBookmarkForm"
      :bookmark="bookmark"
      :isAdd="isAddBookmark"
    />

    <Bookmark v-model="showBookmarkDialog" :book="bookmarkInBook" />
  </div>
</template>

<script>
import Axios from "./plugins/axios";
import eventBus from "./plugins/eventBus";
import ImageViewer from "element-ui/packages/image/src/image-viewer.vue";
import ReplaceRule from "./components/ReplaceRule.vue";
import ReplaceRuleForm from "./components/ReplaceRuleForm.vue";
import MPCode from "./components/MPCode.vue";
import BookManage from "./components/BookManage.vue";
import BookInfo from "./components/BookInfo.vue";
import UserManage from "./components/UserManage.vue";
import AddUser from "./components/AddUser.vue";
import BookGroup from "./components/BookGroup.vue";
import RssSourceList from "./components/RssSourceList.vue";
import RssArticleList from "./components/RssArticleList.vue";
import RssArticle from "./components/RssArticle.vue";
import SearchBookContent from "./components/SearchBookContent.vue";
import Bookmark from "./components/Bookmark.vue";
import BookmarkForm from "./components/BookmarkForm.vue";
import { CodeJar } from "codejar";
import Prism from "prismjs";
import "prismjs/components/prism-json";
import "prismjs/themes/prism.css";
import "./assets/fonts/iconfont.css";
import {
  cacheFirstRequest,
  isMiniInterface,
  networkFirstRequest
} from "./plugins/helper";

Date.prototype.format = function(fmt) {
  var o = {
    "M+": this.getMonth() + 1, //月份
    "d+": this.getDate(), //日
    "h+": this.getHours(), //小时
    "m+": this.getMinutes(), //分
    "s+": this.getSeconds(), //秒
    "q+": Math.floor((this.getMonth() + 3) / 3), //季度
    S: this.getMilliseconds() //毫秒
  };
  if (/(y+)/.test(fmt)) {
    fmt = fmt.replace(
      RegExp.$1,
      (this.getFullYear() + "").substr(4 - RegExp.$1.length)
    );
  }
  for (var k in o) {
    if (new RegExp("(" + k + ")").test(fmt)) {
      fmt = fmt.replace(
        RegExp.$1,
        RegExp.$1.length == 1 ? o[k] : ("00" + o[k]).substr(("" + o[k]).length)
      );
    }
  }
  return fmt;
};

//字符编码数值对应的存储长度：
//UCS-2编码(16进制) UTF-8 字节流(二进制)
//0000 - 007F       0xxxxxxx （1字节）
//0080 - 07FF       110xxxxx 10xxxxxx （2字节）
//0800 - FFFF       1110xxxx 10xxxxxx 10xxxxxx （3字节）
String.prototype.getBytesLength = function() {
  var totalLength = 0;
  var charCode;
  for (var i = 0; i < this.length; i++) {
    charCode = this.charCodeAt(i);
    if (charCode < 0x007f) {
      totalLength++;
    } else if (0x0080 <= charCode && charCode <= 0x07ff) {
      totalLength += 2;
    } else if (0x0800 <= charCode && charCode <= 0xffff) {
      totalLength += 3;
    } else {
      totalLength += 4;
    }
  }
  return totalLength;
};

export default {
  name: "app",
  components: {
    ImageViewer,
    ReplaceRule,
    ReplaceRuleForm,
    MPCode,
    BookManage,
    BookInfo,
    UserManage,
    AddUser,
    BookGroup,
    RssSourceList,
    RssArticleList,
    RssArticle,
    SearchBookContent,
    Bookmark,
    BookmarkForm
  },
  data() {
    return {
      remember: true,
      loginForm: {
        username: "",
        password: "",
        code: ""
      },
      showEditor: false,
      editorTitle: "编辑器",
      editorContent: "",

      showReplaceRuleDialog: false,

      showReplaceRuleForm: false,
      replaceRule: {},
      isAddReplaceRule: true,

      showMPCodeDialog: false,

      showBookManageDialog: false,

      showBookInfoDialog: false,

      showUserManageDialog: false,
      showAddUserDialog: false,

      showBookGroupDialog: false,
      isSetBookGroup: false,

      showRssSourceListDialog: false,
      showRssArticleListDialog: false,
      rssSource: {},
      showRssArticleDialog: false,
      rssArticleInfo: {},

      showSearchBookContentDialog: false,
      searchBook: {},

      isLogin: true,

      showBookmarkDialog: false,

      showBookmarkForm: false,
      bookmark: {},
      isAddBookmark: true,
      bookmarkInBook: {}
    };
  },
  beforeCreate() {
    this.$store.dispatch("syncFromLocalStorage");

    this.$store.commit("setMiniInterface", isMiniInterface());

    document.documentElement.style.setProperty(
      "--vh",
      `${window.innerHeight * 0.01}px`
    );

    window.onresize = () => {
      document.documentElement.style.setProperty(
        "--vh",
        `${window.innerHeight * 0.01}px`
      );
      this.$store.commit("setMiniInterface", isMiniInterface());
      this.$store.commit("setWindowSize", {
        width: window.innerWidth,
        height: window.innerHeight
      });
      this.$store.commit("setTouchable", "ontouchstart" in document);
    };

    const api = window.getQueryString("api");
    if (api) {
      this.$store.commit("setApi", api);
    }

    if (
      window.navigator.userAgent.indexOf("iPhone") >= 0 ||
      window.navigator.userAgent.indexOf("iPad") >= 0
    ) {
      document.documentElement.style.setProperty("height", "100vh");
      document.body.style.setProperty("height", "100vh");
    }

    // window.webAppDistance =
    //   window.navigator.userAgent.indexOf("iPhone") >= 0 &&
    //   window.navigator.standalone
    //     ? (window.devicePixelRatio - 1 || 1) * 20
    //     : 0;
    // document.documentElement.style.setProperty(
    //   "--status-bar-height",
    //   `${window.webAppDistance}px`
    // );

    try {
      const docStyle = getComputedStyle(document.documentElement);
      this.$store.commit("setSafeArea", {
        top: docStyle.getPropertyValue("--sat") | 0,
        bottom: docStyle.getPropertyValue("--sab") | 0,
        left: docStyle.getPropertyValue("--sal") | 0,
        right: docStyle.getPropertyValue("--sar") | 0
      });
    } catch (error) {
      //
    }
  },
  created() {
    window
      .matchMedia("(prefers-color-scheme: dark)")
      .addEventListener("change", () => {
        this.autoSetTheme(this.autoTheme);
      });
    this.autoSetTheme(this.autoTheme);

    this.getUserInfo().then(() => {
      this.$store.dispatch("syncFromLocalStorage");
      this.init();
    });
    this.loadTxtTocRules();
  },
  beforeMount() {
    this.setTheme(this.isNight);
    this.setMiniInterfaceClass();
    this.setPageTypeClass();
    this.eventBus = eventBus;
    eventBus.$on("showEditor", this.showEditorListener);
    eventBus.$on("showReplaceRuleForm", this.showReplaceRuleFormListener);
    eventBus.$on("showReplaceRuleDialog", () => {
      this.showReplaceRuleDialog = true;
    });
    eventBus.$on("showMPCodeDialog", () => {
      this.showMPCodeDialog = true;
    });
    eventBus.$on("showBookManageDialog", () => {
      this.showBookManageDialog = true;
    });
    eventBus.$on("showBookInfoDialog", book => {
      this.showBookInfo = book;
      this.showBookInfoDialog = true;
    });
    eventBus.$on("showUserManageDialog", () => {
      this.showUserManageDialog = true;
    });
    eventBus.$on("showAddUserDialog", () => {
      this.showAddUserDialog = true;
    });
    eventBus.$on("showBookGroupDialog", isSet => {
      this.showBookGroupDialog = true;
      this.isSetBookGroup = !!isSet;
    });
    eventBus.$on("showRssArticleListDialog", rssSource => {
      this.showRssArticleListDialog = true;
      this.rssSource = rssSource;
    });
    eventBus.$on("showRssSourceListDialog", () => {
      this.showRssSourceListDialog = true;
    });
    eventBus.$on("showRssArticleDialog", rssArticleInfo => {
      this.showRssArticleDialog = true;
      this.rssArticleInfo = rssArticleInfo;
    });
    eventBus.$on("showSearchBookContentDialog", searchBook => {
      this.showSearchBookContentDialog = true;
      this.searchBook = searchBook;
    });
    eventBus.$on("showBookmarkForm", (bookmark, isAddBookmark, callback) => {
      this.bookmark = bookmark;
      this.isAddBookmark = isAddBookmark;
      this.bookmarkCallback = callback;
      this.showBookmarkForm = true;
    });
    eventBus.$on("showBookmarkDialog", book => {
      this.showBookmarkDialog = true;
      this.bookmarkInBook = book;
    });
  },
  mounted() {
    document.documentElement.style.setProperty(
      "--vh",
      `${window.innerHeight * 0.01}px`
    );
    window.reader = this;
  },
  computed: {
    isNight() {
      return this.$store.getters.isNight;
    },
    autoTheme() {
      return this.$store.getters.config.autoTheme;
    },
    dialogWidth() {
      return this.$store.getters.dialogSmallWidth;
    },
    dialogTop() {
      return this.$store.getters.dialogTop;
    },
    showLogin: {
      get() {
        return this.$store.state.showLogin;
      },
      set(value) {
        this.$store.commit("setShowLogin", value);
      }
    },
    connected() {
      return this.$store.state.connected;
    },
    showBookInfo: {
      get() {
        return this.$store.state.showBookInfo;
      },
      set(val) {
        this.$store.commit("setShowBookInfo", val);
      }
    }
  },
  watch: {
    isNight(val) {
      this.setTheme(val);
    },
    autoTheme(val) {
      this.autoSetTheme(val);
    },
    miniInterface() {
      this.setMiniInterfaceClass();
    },
    connected(val) {
      if (val) {
        // 连接后端成功，加载自定义样式
        window.customCSSLoad ||
          window.loadLink(this.$store.getters.customCSSUrl, () => {
            window.customCSSLoad = true;
          });
      }
    },
    "$store.state.config.pageType": function() {
      this.setPageTypeClass();
    },
    showReplaceRuleForm(val) {
      if (!val) {
        if (this.replaceRuleCallback) {
          this.replaceRuleCallback();
          this.replaceRuleCallback = null;
        }
      }
    },
    showBookmarkForm(val) {
      if (!val) {
        if (this.bookmarkCallback) {
          this.bookmarkCallback();
          this.bookmarkCallback = null;
        }
      }
    },
    showLogin(val) {
      if (!val) {
        this.isLogin = true;
      }
    }
  },
  methods: {
    autoSetTheme(autoTheme) {
      if (autoTheme) {
        if (window.matchMedia("(prefers-color-scheme: dark)").matches) {
          // 是暗色模式
          this.$store.commit("setNightTheme", true);
        } else {
          // 非暗色模式
          this.$store.commit("setNightTheme", false);
        }
      }
    },
    setTheme(isNight) {
      if (isNight) {
        document.body.className =
          (document.body.className || "").replace("night-theme", "") +
          " night-theme";
      } else {
        document.body.className = (document.body.className || "").replace(
          "night-theme",
          ""
        );
      }
    },
    setMiniInterfaceClass() {
      if (this.$store.state.miniInterface) {
        document.body.className =
          (document.body.className || "").replace("mini-interface", "") +
          " mini-interface";
      } else {
        document.body.className = (document.body.className || "").replace(
          "mini-interface",
          ""
        );
      }
    },
    setPageTypeClass() {
      if (this.$store.getters.isKindlePage) {
        document.body.className =
          (document.body.className || "").replace("kindle-page", "") +
          " kindle-page";
      } else {
        document.body.className = (document.body.className || "").replace(
          "kindle-page",
          ""
        );
      }
    },
    cancel() {
      this.showLogin = false;
      this.loginForm = {
        username: "",
        password: "",
        code: ""
      };
    },
    async login() {
      const res = await Axios.post("/login", {
        ...this.loginForm,
        isLogin: this.isLogin
      });
      if (res.data.isSuccess) {
        this.$store.commit("setShowLogin", false);
        this.$nextTick(() => {
          this.$store.commit("setLoginAuth", true);
        });
        if (this.remember && res.data.data && res.data.data.accessToken) {
          this.$store.commit("setToken", res.data.data.accessToken);
        }
        this.getUserInfo().then(() => {
          this.$store.dispatch("syncFromLocalStorage");
          this.init(true);
        });
      }
    },
    async init(refresh) {
      if (this.initing) {
        refresh &&
          setTimeout(() => {
            this.init(refresh);
          }, 100);
        return;
      }
      this.initing = true;
      if (refresh || !this.$store.state.shelfBooks.length) {
        await this.loadBookShelf().catch(() => {
          this.initing = false;
        });
      }
      await Promise.all([
        // 加载书源列表
        this.loadBookSource(refresh),
        // 加载分组列表
        this.loadBookGroup(refresh),
        // 加载RSS订阅列表
        this.loadRssSources(refresh),
        // 加载替换规则
        this.loadReplaceRules(refresh),
        // 加载书签
        this.loadBookmarks(refresh)
      ]);
      // 加载书架
      this.initing = false;
    },
    getUserInfo() {
      return networkFirstRequest(
        () => Axios.get(this.api + "/getUserInfo"),
        "userInfo"
      ).then(
        res => {
          this.$store.commit("setConnected", true);
          if (res.data.isSuccess) {
            this.$store.commit("setIsSecureMode", res.data.data.secure);
            if (res.data.data.secure && res.data.data.secureKey) {
              this.$store.commit("setShowManagerMode", true);
            }
            if (res.data.data.userInfo) {
              this.$store.commit("setUserInfo", res.data.data.userInfo);
            }
          }
        },
        error => {
          this.$message.error(
            "加载用户信息失败 " + (error && error.toString())
          );
        }
      );
    },
    loadTxtTocRules() {
      return cacheFirstRequest(
        () => Axios.get("/getTxtTocRules"),
        "txtTocRules"
      ).then(
        res => {
          const data = res.data.data || [];
          this.$store.commit("setTxtTocRules", data);
        },
        error => {
          this.$message.error(
            "加载txt章节规则失败 " + (error && error.toString())
          );
        }
      );
    },
    showEditorListener(title, content, callback) {
      this.editorTitle = title;
      this.editorContent = content;
      this.showEditor = true;
      this.callback = callback;
      this.$nextTick(() => {
        this.initEditor();
      });
    },
    showReplaceRuleFormListener(replaceRule, isAddReplaceRule, callback) {
      this.replaceRule = replaceRule;
      this.isAddReplaceRule = isAddReplaceRule;
      this.replaceRuleCallback = callback;
      this.showReplaceRuleForm = true;
    },
    loadBookShelf(refresh, api) {
      api = api || this.api;
      return networkFirstRequest(
        () => Axios.get(api + "/getBookshelf?refresh=" + (refresh ? 1 : 0)),
        "getBookshelf@" + this.currentUserName
      )
        .then(response => {
          this.$store.commit("setConnected", true);
          if (response.data.isSuccess) {
            this.$store.commit("setShelfBooks", response.data.data);
          }
        })
        .catch(error => {
          this.$store.commit("setConnected", false);
          this.$message.error("后端连接失败 " + (error && error.toString()));
          throw error;
        });
    },
    loadBookGroup(refresh) {
      return cacheFirstRequest(
        () => Axios.get(this.api + "/getBookGroups"),
        "bookGroup@" + this.currentUserName,
        refresh
      ).then(
        res => {
          if (res.data.isSuccess) {
            this.$store.commit("setBookGroupList", res.data.data || []);
          }
        },
        error => {
          this.$message.error(
            "加载分组列表失败 " + (error && error.toString())
          );
        }
      );
    },
    loadRssSources(refresh) {
      return cacheFirstRequest(
        () =>
          Axios.get(this.api + "/getRssSources", {
            params: {
              simple: 1
            }
          }),
        "rssSources@" + this.currentUserName,
        refresh
      ).then(
        res => {
          const data = res.data.data || [];
          this.$store.commit("setRssSourceList", data);
        },
        error => {
          this.$message.error(
            "加载RSS订阅列表失败 " + (error && error.toString())
          );
        }
      );
    },
    loadBookSource(refresh) {
      return cacheFirstRequest(
        () =>
          Axios.get(this.api + "/getBookSources", {
            params: {
              simple: 1
            }
          }),
        "bookSourceList@" + this.currentUserName,
        refresh
      ).then(
        res => {
          if (res.data.isSuccess) {
            this.$store.commit("setBookSourceList", res.data.data || []);
          }
        },
        error => {
          this.$message.error(
            "加载书源列表失败 " + (error && error.toString())
          );
        }
      );
    },
    loadReplaceRules(refresh) {
      return cacheFirstRequest(
        () => Axios.get(this.api + "/getReplaceRules"),
        "replaceRule@" + this.currentUserName,
        refresh
      ).then(
        res => {
          if (res.data.isSuccess) {
            this.$store.commit("setFilterRules", res.data.data || []);
          }
        },
        error => {
          this.$message.error(
            "加载替换规则失败 " + (error && error.toString())
          );
        }
      );
    },
    loadBookmarks(refresh) {
      return cacheFirstRequest(
        () => Axios.get(this.api + "/getBookmarks"),
        "bookmark@" + this.currentUserName,
        refresh
      ).then(
        res => {
          if (res.data.isSuccess) {
            this.$store.commit("setBookmarks", res.data.data || []);
          }
        },
        error => {
          this.$message.error("加载书签失败 " + (error && error.toString()));
        }
      );
    },
    async isInShelf(book, addTip) {
      if (!book || !book.bookUrl || !book.origin) {
        this.$message.error("书籍信息错误");
        return false;
      }
      // 判断是否加入了书架
      const isInShelf = this.$store.getters.shelfBooks.find(
        v => v.bookUrl === book.bookUrl
      );
      if (!isInShelf) {
        if (addTip) {
          const res = await this.$confirm(addTip, "提示", {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            type: "warning"
          }).catch(() => {
            return false;
          });
          if (!res) {
            return false;
          }
          // 加入书架
          return Axios.post(this.api + "/saveBook", book).then(
            res => {
              if (res.data.isSuccess) {
                return true;
              }
            },
            () => {
              this.$message.error("导入书籍失败");
              return false;
            }
          );
        }
      }
      return !!isInShelf;
    },
    getBookContent(chapterIndex, options, refresh, cache, book) {
      book = book || {
        name: this.$store.getters.readingBook.name,
        author: this.$store.getters.readingBook.author,
        bookUrl: this.$store.getters.readingBook.bookUrl
      };
      const params = {
        url: book.bookUrl,
        index: chapterIndex
      };
      if (refresh) {
        params.refresh = 1;
      }
      if (cache) {
        params.cache = 1;
      }
      return cacheFirstRequest(
        () =>
          Axios.post(this.api + "/getBookContent", params, {
            timeout: 30000,
            ...options
          }),
        book.name +
          "_" +
          book.author +
          "@" +
          book.bookUrl +
          "@chapterContent-" +
          chapterIndex,
        refresh
      );
    },
    initEditor() {
      const editor = this.$refs.editorRef;
      if (!editor) {
        setTimeout(() => {
          this.initEditor();
        }, 10);
      }
      try {
        this.jar = CodeJar(editor, Prism.highlightElement, { tab: "\t" });

        // Update code
        this.jar.updateCode(this.editorContent);

        // Listen to updates
        this.jar.onUpdate(code => {
          // console.log(code);
          this.editorContent = code;
        });
      } catch (e) {
        //
      }
    },
    closeEditor() {
      this.jar && this.jar.destroy && this.jar.destroy();
      this.showEditor = false;
      this.editorTitle = "";
      this.editorContent = "";
      this.callback = null;
    },
    saveEditor() {
      if (this.callback) {
        this.callback(this.editorContent, () => {
          this.closeEditor();
        });
      }
    },
    closeViewer() {
      this.$store.commit("setPreviewImgList", false);
    }
  }
};
</script>

<style>
:root {
  --ui-radius: 12px;
  --ui-radius-sm: 8px;
  --ui-radius-lg: 16px;
  --ui-transition: 220ms cubic-bezier(.4,0,.2,1);
  --ui-shadow-sm: 0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04);
  --ui-shadow: 0 4px 16px rgba(0,0,0,.08), 0 1px 3px rgba(0,0,0,.06);
  --ui-shadow-lg: 0 12px 40px rgba(0,0,0,.12), 0 4px 12px rgba(0,0,0,.06);
  --ui-accent: #4f6ef7;
  --ui-accent-hover: #3d5ce5;
  --ui-surface: #ffffff;
  --ui-bg: #f5f6f8;
  --ui-text: #1a1d23;
  --ui-text-secondary: #5f6672;
  --ui-text-muted: #9ba3ae;
  --ui-border: rgba(0,0,0,.08);
  --ui-font: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB",
    "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei",
    "Helvetica Neue", Helvetica, Arial, sans-serif;
}

#app {
  font-family: var(--ui-font);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--ui-text);
  margin: 0;
  height: 100%;
  position: relative;
}

@font-face {
  font-family: "reader-st";
  src: local("Songti SC"), local("Noto Serif CJK SC"),
    local("Source Han Serif SC"), local("Source Han Serif CN"), local("STSong"),
    local("宋体"), local("明体"), local("明朝"), local("Songti"),
    local("Songti TC"), local("Song S"), local("Song T"),
    local("STBShusong"), local("TBMincho"), local("HYMyeongJo"),
    local("DK-SONGTI");
}

@font-face {
  font-family: "reader-fs";
  src: local("STFangsong"), local("FangSong"), local("FangSong_GB2312"),
    local("amasis30"), local("仿宋"), local("仿宋_GB2312"), local("Yuanti"),
    local("Yuanti SC"), local("Yuanti TC"),
    local("DK-FANGSONG");
}

@font-face {
  font-family: "reader-kt";
  src: local("Kaiti SC"), local("STKaiti"), local("Caecilia"), local("楷体"),
    local("楷体_GB2312"), local("Kaiti"), local("Kaiti SC"), local("Kaiti TC"),
    local("MKai PRC"), local("MKaiGB18030C-Medium"),
    local("MKaiGB18030C-Bold"), local("DK-KAITI");
}

@font-face {
  font-family: "reader-ht";
  src: local("Noto Sans CJK SC"), local("Source Han Sans SC"),
    local("Source Han Sans CN"), local("Microsoft YaHei"), local("PingFang SC"),
    local("Hiragino Sans GB"), local("黑体"), local("微软雅黑"), local("Heiti"),
    local("Heiti SC"), local("Heiti TC"), local("MYing Hei S"),
    local("MYing Hei T"), local("TBGothic"),
    local("DK-HEITI");
}

*::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
*::-webkit-scrollbar-track {
  background: transparent;
}
*::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,.15);
  border-radius: 3px;
}
*::-webkit-scrollbar-thumb:hover {
  background: rgba(0,0,0,.25);
}
*:focus {
  outline: none !important;
}

.el-dialog {
  border-radius: var(--ui-radius-lg) !important;
  box-shadow: var(--ui-shadow-lg) !important;
  overflow: hidden;
}
.el-dialog .el-dialog__header {
  padding: 22px 24px 14px;
}
.el-dialog__header .el-dialog__headerbtn {
  margin: 0;
  font-size: 20px;
  line-height: 24px;
  top: 18px;
  right: 20px;
  transition: transform var(--ui-transition);
}
.el-dialog__header .el-dialog__headerbtn:hover {
  transform: scale(1.15);
}
.el-dialog__title {
  font-weight: 600;
  font-size: 17px;
}
.el-dialog__body {
  padding: 16px 24px;
}
.el-dialog__footer {
  padding: 12px 24px 20px;
}

.el-button {
  border-radius: var(--ui-radius-sm) !important;
  font-weight: 500;
  transition: all var(--ui-transition) !important;
}
.el-button--primary {
  background: var(--ui-accent) !important;
  border-color: var(--ui-accent) !important;
}
.el-button--primary:hover, .el-button--primary:focus {
  background: var(--ui-accent-hover) !important;
  border-color: var(--ui-accent-hover) !important;
}

.el-input__inner {
  border-radius: var(--ui-radius-sm) !important;
  transition: border-color var(--ui-transition), box-shadow var(--ui-transition) !important;
}
.el-input__inner:focus {
  box-shadow: 0 0 0 3px rgba(79,110,247,.15) !important;
}

.el-select-dropdown {
  border-radius: var(--ui-radius) !important;
  box-shadow: var(--ui-shadow-lg) !important;
  border: 1px solid var(--ui-border) !important;
  overflow: hidden;
}

.el-message-box {
  border-radius: var(--ui-radius-lg) !important;
  box-shadow: var(--ui-shadow-lg) !important;
  border: none !important;
}
.el-message-box__btns .el-button {
  min-width: 72px;
}

.el-tag {
  border-radius: 6px !important;
  font-weight: 500;
}

.el-tabs__active-bar {
  border-radius: 2px;
}

.el-checkbox__inner {
  border-radius: 4px;
}

.el-pagination {
  font-weight: 500;
}
.el-pager li {
  border-radius: 6px !important;
  min-width: 30px;
}
</style>
<style lang="stylus">
.popper-component {
  top: 0 !important;
}
.code-editor {
  max-height: calc(var(--vh, 1vh) * 80 - 54px - 60px - 66px);
  overflow-y: auto;
  border-radius: var(--ui-radius-sm);
}
.mini-interface {
  .popper-component {
    top: 0 !important;
    left: 0 !important;
    width: 100vw !important;
    box-sizing: border-box;
    margin: 0 !important;
    overflow-x: hidden;
  }
  .code-editor {
    max-height: calc(var(--vh, 1vh) * 100 - 54px - 40px - 66px);
  }
}
.night-theme {
  --ui-surface: #1e1e24;
  --ui-bg: #16161a;
  --ui-text: #e2e4e8;
  --ui-text-secondary: #a0a4ae;
  --ui-text-muted: #6b7080;
  --ui-border: rgba(255,255,255,.08);
  --ui-accent: #6b8aff;
  --ui-accent-hover: #5a7af0;
  background-color: var(--ui-bg);

  *::-webkit-scrollbar-thumb {
    background: rgba(255,255,255,.15);
  }
  *::-webkit-scrollbar-thumb:hover {
    background: rgba(255,255,255,.25);
  }

  .el-message-box {
    background: #1e1e24;
    border: 1px solid rgba(255,255,255,.06);
    .el-message-box__title {
      color: #c5c8ce;
    }
    .el-message-box__content {
      color: #9ba3ae;
    }
  }
  .el-button--default {
    background: #2a2b32;
    color: #c5c8ce;
    border: 1px solid rgba(255,255,255,.1);
  }
  .el-button:focus, .el-button:hover {
      color: #e8eaee;
      border-color: rgba(255,255,255,.2);
      background-color: #35363e;
  }
  .el-button--text:focus, .el-button--text:hover {
      color: var(--ui-accent);
      border-color: transparent;
      background-color: transparent;
  }
  .el-button.is-disabled, .el-button.is-disabled:focus, .el-button.is-disabled:hover {
      color: #555;
  }
  .el-button--primary {
    background: var(--ui-accent) !important;
    border: 1px solid var(--ui-accent) !important;
  }
  .el-button--primary:focus, .el-button--primary:hover {
      background: var(--ui-accent-hover) !important;
      border-color: var(--ui-accent-hover) !important;
      color: #FFF;
  }
  .el-input-number__increase, .el-input-number__decrease {
      background-color: #35363e;
      border-color: rgba(255,255,255,.08);
      color: #c5c8ce;
  }
  .el-checkbox__inner {
    background: #35363e;
    border-color: rgba(255,255,255,.15);
  }
  .el-input__inner {
    background-color: #2a2b32;
    border: 1px solid rgba(255,255,255,.08) !important;
    color: #e2e4e8;
  }
  .el-input__inner:focus {
    box-shadow: 0 0 0 3px rgba(107,138,255,.2) !important;
  }
  .el-textarea__inner {
    background-color: #2a2b32;
    border: 1px solid rgba(255,255,255,.08) !important;
    color: #e2e4e8;
  }
  .el-tabs__item {
    color: #9ba3ae;
  }
  .el-tabs__item.is-active {
    color: var(--ui-accent);
  }
  .el-tabs__nav-next, .el-tabs__nav-prev {
    color: #6b7080;
  }
  .el-tabs__nav-wrap::after {
    background-color: rgba(255,255,255,.06);
  }
  .el-tabs__active-bar {
    background-color: var(--ui-accent);
  }
  .el-select-dropdown {
    background-color: #26272e;
    border: 1px solid rgba(255,255,255,.08) !important;
  }
  .el-select-dropdown__item {
    color: #c5c8ce;
  }
  .el-select-dropdown__item.hover, .el-select-dropdown__item:hover {
    background-color: #2e2f38;
  }
  .el-select .el-tag.el-tag--info {
    background-color: #35363e;
    border-color: rgba(255,255,255,.1);
    color: #c5c8ce;
  }
  .el-select-dropdown.is-multiple .el-select-dropdown__item.selected.hover,
  .el-select-dropdown.is-multiple .el-select-dropdown__item.hover {
    background-color: #35363e;
  }
  .el-select-dropdown.is-multiple .el-select-dropdown__item.selected {
    background-color: #2e2f38;
  }
  .el-popper[x-placement^="bottom"] .popper__arrow, .el-popper[x-placement^="bottom"] .popper__arrow::after {
    border-bottom-color: #26272e !important;
  }
  .el-popper[x-placement^="top"] .popper__arrow, .el-popper[x-placement^="top"] .popper__arrow::after {
    border-top-color: #26272e !important;
  }
  .el-dialog {
    background-color: #1e1e24;
  }
  .el-dialog__title {
    color: #e2e4e8;
  }
  .el-pagination .btn-next, .el-pagination .btn-prev {
    background: center center no-repeat #2a2b32;
    color: #c5c8ce;
  }
  .el-pager li {
    background: #2a2b32;
    color: #9ba3ae;
  }
  .el-pager li.btn-quicknext, .el-pager li.btn-quickprev {
    color: #9ba3ae;
  }
  .el-pager li.active {
    color: var(--ui-accent);
  }
  .code-editor {
    .token.operator,
    .token.entity,
    .token.url,
    .language-css .token.string,
    .style .token.string {
      background: inherit;
    }
  }

  .el-table {
    background-color: transparent;
    color: #9ba3ae;
  }
  .el-table__expanded-cell {
    background-color: transparent;
  }
  .el-table th, .el-table tr{
    background-color: #1e1e24 !important;
  }
  .el-table td {
    border-bottom: 1px solid rgba(255,255,255,.06);
  }
  .el-table th.is-leaf {
    border-bottom: 1px solid rgba(255,255,255,.06);
  }
  .el-table td.el-table__cell, .el-table th.el-table__cell.is-leaf {
    border-bottom: 1px solid rgba(255,255,255,.06);
  }
  .el-dropdown-menu {
    background-color: #26272e !important;
    border-color: rgba(255,255,255,.08);
    border-radius: var(--ui-radius);
  }
  .el-dropdown-menu__item:focus, .el-dropdown-menu__item:not(.is-disabled):hover {
    background-color: #35363e !important;
    border-color: transparent;
  }
  .el-dropdown-menu__item {
    color: #c5c8ce;
  }
  .el-table--border::after,
  .el-table--group::after,
  .el-table::before {
    background-color: transparent;
  }
  .el-table--enable-row-hover .el-table__body tr:hover>td {
    background-color: #26272e;
  }
  .el-table__fixed-right::before, .el-table__fixed::before {
    background-color: #26272e;
  }
  .el-table__body tr.hover-row.current-row>td,
  .el-table__body tr.hover-row.el-table__row--striped.current-row>td,
  .el-table__body tr.hover-row.el-table__row--striped>td,
  .el-table__body tr.hover-row>td {
    background-color: #2a2b32;
  }
  .el-table__body tr.hover-row.current-row>td.el-table__cell,
  .el-table__body tr.hover-row.el-table__row--striped.current-row>td.el-table__cell,
  .el-table__body tr.hover-row.el-table__row--striped>td.el-table__cell,
  .el-table__body tr.hover-row>td.el-table__cell {
    background-color: #2a2b32;
    color: #e2e4e8;
  }
  .el-table--enable-row-hover .el-table__body tr:hover>td.el-table__cell {
    background-color: #2a2b32;
    color: #e2e4e8;
  }

  .check-tip {
    color: #9ba3ae;
  }

  .el-form-item__label {
    color: #9ba3ae;
  }
  .el-checkbox__label {
    color: #9ba3ae;
  }
}
.el-popover:focus, .el-popover:focus:active, .el-popover__reference:focus:hover, .el-popover__reference:focus:not(.focusing) {
  outline: none;
}
.el-message-box {
  max-width: 85vw;
}
.el-dialog__header {
  position: relative;
}
.el-dialog.is-fullscreen {
  padding-top: 0;
  padding-top: constant(safe-area-inset-top) !important;
  padding-top: env(safe-area-inset-top) !important;
  border-radius: 0 !important;
}
.popper-component.el-popover {
  border: none;
  box-shadow: none;
}
.kindle-page {
  -webkit-tap-highlight-color: rgba(255, 255, 255, 0);
  -webkit-user-select: none;
}
.check-tip {
  display: inline-block;
  float: left;
  line-height: 40px;
  margin-left: 10px;
  font-size: 14px;
}
.float-left {
  float: left;
}
.float-right {
  float: right;
}
.custom-dialog-title {
  .span-btn {
    display: inline-block;
    cursor: pointer;
    font-size: 14px;
    margin-right: 12px;
    color: var(--ui-accent, #4f6ef7);
    font-weight: 500;
    transition: opacity var(--ui-transition);
    &:hover {
      opacity: 0.75;
    }
  }
}
</style>
