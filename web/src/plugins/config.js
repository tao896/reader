import body_0 from "../assets/imgs/themes/body_0.png";
import content_0 from "../assets/imgs/themes/content_0.png";
import popup_0 from "../assets/imgs/themes/popup_0.png";
import body_1 from "../assets/imgs/themes/body_1.png";
import content_1 from "../assets/imgs/themes/content_1.png";
import popup_1 from "../assets/imgs/themes/popup_1.png";
import body_2 from "../assets/imgs/themes/body_2.png";
import content_2 from "../assets/imgs/themes/content_2.png";
import popup_2 from "../assets/imgs/themes/popup_2.png";
import body_3 from "../assets/imgs/themes/body_3.png";
import content_3 from "../assets/imgs/themes/content_3.png";
import popup_3 from "../assets/imgs/themes/popup_3.png";
import body_5 from "../assets/imgs/themes/body_5.png";
import content_5 from "../assets/imgs/themes/content_5.png";
import popup_5 from "../assets/imgs/themes/popup_5.png";
import body_6 from "../assets/imgs/themes/body_6.png";
import content_6 from "../assets/imgs/themes/content_6.png";
// import popup_6 from "../assets/imgs/themes/popup_6.png";

const defaultDayConfig = {
  configDefaultType: "白天默认",
  name: "内置白天",
  theme: 0,
  font: 0,
  chineseFont: "简体",
  fontSize: 18,
  fontWeight: 400,
  fontColor: "#262626",
  bodyColor: "#eadfca",
  contentColor: "#fff",
  popupColor: "#ede7da",
  themeType: "day",
  readMethod: "上下滑动",
  clickMethod: "自动",
  animateMSTime: 300, // 翻页动画时长
  readWidth: 800,
  lineHeight: 1.8, // 行高
  paragraphSpace: 0.2, // 段间距
  autoReadingMethod: "像素滚动",
  autoReadingPixel: 1,
  autoReadingLineTime: 1000,
  pageMode: "自适应",
  selectionAction: "操作弹窗"
};
const defaultNightConfig = {
  configDefaultType: "黑夜默认",
  name: "内置黑夜",
  theme: 6,
  font: 0,
  chineseFont: "简体",
  fontSize: 18,
  fontWeight: 400,
  fontColor: "#666666",
  bodyColor: "#121212",
  contentColor: "#171717",
  popupColor: "#121212",
  themeType: "night",
  readMethod: "上下滑动",
  clickMethod: "自动",
  animateMSTime: 300, // 翻页动画时长
  readWidth: 800,
  lineHeight: 1.8, // 行高
  paragraphSpace: 0.2, // 段间距
  autoReadingMethod: "像素滚动",
  autoReadingPixel: 1,
  autoReadingLineTime: 1000,
  pageMode: "自适应",
  selectionAction: "操作弹窗"
};
const settings = {
  shelfConfig: {
    showBookGroup: -1
  },
  searchConfig: {
    searchType: "multi",
    bookSourceGroup: "",
    bookSourceUrl: "",
    concurrentCount: 24
  },
  customConfigList: [defaultDayConfig, defaultNightConfig],
  config: {
    ...defaultDayConfig,
    customConfig: "内置白天",
    autoTheme: true, // 自动切换主题
    pageType: "正常"
  },
  speechVoiceConfig: {
    voiceName: "",
    speechRate: 1,
    speechPitch: 1
  },
  defaultNightTheme: 6,
  themes: [
    {
      body: "url(" + body_0 + ") repeat",
      content: "url(" + content_0 + ") repeat",
      popup: "url(" + popup_0 + ") repeat"
    },
    {
      body: "url(" + body_1 + ") repeat",
      content: "url(" + content_1 + ") repeat",
      popup: "url(" + popup_1 + ") repeat"
    },
    {
      body: "url(" + body_2 + ") repeat",
      content: "url(" + content_2 + ") repeat",
      popup: "url(" + popup_2 + ") repeat"
    },
    {
      body: "url(" + body_3 + ") repeat",
      content: "url(" + content_3 + ") repeat",
      popup: "url(" + popup_3 + ") repeat"
    },
    {
      body: "#ebcece repeat",
      content: "#f5e4e4 repeat",
      popup: "#faeceb repeat"
    },
    {
      body: "url(" + body_5 + ") repeat",
      content: "url(" + content_5 + ") repeat",
      popup: "url(" + popup_5 + ") repeat"
    },
    {
      body: "url(" + body_6 + ") repeat",
      content: "url(" + content_6 + ") repeat",
      popup: "#121212"
    },
    {
      body: "#f7f7f7 repeat",
      content: "#fff repeat",
      popup: "#f7f7f7 repeat"
    },
    {
      name: "墨韵纸书",
      scope: "pc",
      pcOnly: true,
      themeType: "day",
      preview: ["#f5f1e8", "#fffaf0", "#b94d2c", "#2f6f73"],
      body: "linear-gradient(135deg, #f5f1e8 0%, #ede4d4 100%)",
      content: {
        backgroundColor: "#fffaf0",
        backgroundImage:
          "linear-gradient(180deg, rgba(255,255,255,0.74), rgba(244,235,218,0.72))",
        boxShadow: "0 22px 70px rgba(90, 62, 28, 0.16)"
      },
      popup: "rgba(255, 252, 245, 0.94)",
      popupPure: "#fffaf0",
      fontColor: "#25201b"
    },
    {
      name: "冷静效率",
      scope: "pc",
      pcOnly: true,
      themeType: "day",
      preview: ["#f4f6f8", "#ffffff", "#2563eb", "#0f9f8f"],
      body: "linear-gradient(135deg, #f4f6f8 0%, #e8edf1 100%)",
      content: {
        backgroundColor: "#ffffff",
        backgroundImage:
          "linear-gradient(180deg, rgba(255,255,255,0.98), rgba(240,245,248,0.88))",
        boxShadow: "0 18px 54px rgba(42, 59, 74, 0.14)"
      },
      popup: "rgba(255, 255, 255, 0.96)",
      popupPure: "#ffffff",
      fontColor: "#18212b"
    },
    {
      name: "夜航阅读",
      scope: "pc",
      pcOnly: true,
      themeType: "night",
      preview: ["#11151c", "#222c3b", "#f5b849", "#58d5c9"],
      body: "linear-gradient(135deg, #11151c 0%, #1a2130 100%)",
      content: {
        backgroundColor: "#222c3b",
        backgroundImage:
          "linear-gradient(180deg, rgba(36,45,61,0.98), rgba(20,25,34,0.92))",
        boxShadow: "0 28px 80px rgba(0, 0, 0, 0.36)"
      },
      popup: "rgba(28, 35, 48, 0.94)",
      popupPure: "#222c3b",
      fontColor: "#e9edf2"
    },
    {
      name: "玻璃晨雾",
      scope: "pc",
      pcOnly: true,
      themeType: "day",
      preview: ["#edf7f8", "#ffffff", "#0284a8", "#d65b7f"],
      body: "linear-gradient(135deg, #edf7f8 0%, #f8f1ee 100%)",
      content: {
        backgroundColor: "rgba(255, 255, 255, 0.82)",
        backgroundImage:
          "linear-gradient(180deg, rgba(255,255,255,0.78), rgba(224,244,244,0.66))",
        boxShadow: "0 24px 70px rgba(61, 113, 125, 0.19)",
        backdropFilter: "blur(18px)"
      },
      popup: "rgba(255, 255, 255, 0.66)",
      popupPure: "rgba(255, 255, 255, 0.86)",
      fontColor: "#1e2d35"
    },
    {
      name: "活力漫画",
      scope: "pc",
      pcOnly: true,
      themeType: "day",
      preview: ["#fff6d7", "#ffffff", "#ff4d5e", "#1d85ff"],
      body: "linear-gradient(135deg, #fff6d7 0%, #d8f4ff 100%)",
      content: {
        backgroundColor: "#fffef9",
        backgroundImage:
          "linear-gradient(180deg, rgba(255,255,255,1), rgba(255,240,184,0.55))",
        boxShadow: "8px 8px 0 rgba(22, 22, 22, 0.16)",
        border: "2px solid rgba(22, 22, 22, 0.22)"
      },
      popup: "#ffffff",
      popupPure: "#ffffff",
      fontColor: "#161616"
    },
    {
      name: "静谧森林",
      scope: "pc",
      pcOnly: true,
      themeType: "day",
      preview: ["#eef4e5", "#fbfff1", "#3f7d4a", "#a86f35"],
      body: "linear-gradient(135deg, #eef4e5 0%, #dfe8d1 100%)",
      content: {
        backgroundColor: "#fbfff1",
        backgroundImage:
          "linear-gradient(180deg, rgba(253,255,247,0.96), rgba(224,235,207,0.72))",
        boxShadow: "0 24px 62px rgba(51, 86, 50, 0.17)"
      },
      popup: "rgba(253, 255, 247, 0.94)",
      popupPure: "#fbfff1",
      fontColor: "#223126"
    }
  ],
  fonts: [
    {
      fontFamily: "custom-system"
    },
    // 黑体
    {
      // fontFamily:
      //   '-apple-system, "Noto Sans", "Helvetica Neue", Helvetica, "Nimbus Sans L", Arial, "Liberation Sans", "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", "Source Han Sans SC", "Source Han Sans CN", "Microsoft YaHei", "Wenquanyi Micro Hei", "WenQuanYi Zen Hei", "ST Heiti", SimHei, "WenQuanYi Zen Hei Sharp", sans-serif'
      fontFamily: "custom-ht, reader-ht"
    },
    // 楷体
    {
      // fontFamily:
      // 'Baskerville, Georgia, "Liberation Serif", "Kaiti SC", STKaiti, "AR PL UKai CN", "AR PL UKai HK", "AR PL UKai TW", "AR PL UKai TW MBE", "AR PL KaitiM GB", KaiTi, KaiTi_GB2312, DFKai-SB, "TW-Kai", serif',
      fontFamily: "custom-kt, reader-kt"
      // fontFamily: "STKaiti",
      // "-fx-font-family": "STKaiti"
    },
    // 宋体
    {
      // fontFamily:
      // 'Georgia, "Nimbus Roman No9 L", "Songti SC", "Noto Serif CJK SC", "Source Han Serif SC", "Source Han Serif CN", STSong, "AR PL New Sung", "AR PL SungtiL GB", NSimSun, SimSun, "TW-Sung", "WenQuanYi Bitmap Song", "AR PL UMing CN", "AR PL UMing HK", "AR PL UMing TW", "AR PL UMing TW MBE", PMingLiU, MingLiU, serif',
      fontFamily: "custom-st, reader-st"
      // fontFamily: "'Source Han Serif CN'",
      // "-fx-font-family": "'Source Han Serif CN'"
    },
    // 仿宋
    {
      // fontFamily:
      //   'Baskerville, "Times New Roman", "Liberation Serif", STFangsong, FangSong, FangSong_GB2312, "CWTEX-F", serif',
      fontFamily: "custom-fs, reader-fs"
      // fontFamily: "STFangsong",
      // "-fx-font-family": "STFangsong"
    }
  ]
};
export const errorTypeList = [
  "UnknownHostException",
  "ConnectException: Failed to connect",
  "SocketException: Connection reset",
  "SSLHandshakeException",
  "responseCode: 307",
  "responseCode: 400",
  "responseCode: 403",
  "responseCode: 404",
  "responseCode: 500",
  "responseCode: 502",
  "responseCode: 503",
  "responseCode: 504",
  "responseCode: 513"
];
export const defaultReplaceRule = {
  name: "",
  pattern: "",
  replacement: "",
  scope: "",
  isRegex: false,
  isEnabled: true
};
export const defaultBookmark = {
  bookName: "",
  bookAuthor: "",
  chapterIndex: 0,
  chapterPos: 0,
  chapterName: "",
  bookText: "",
  content: ""
};
export const syncConfigFiled = Object.keys(defaultDayConfig).concat([
  "contentBGImg"
]);
export const customFonts = [
  "custom-system",
  "custom-ht",
  "custom-kt",
  "custom-st",
  "custom-fs"
];
export default settings;
