<template>
  <div
    class="index-wrapper"
    :style="pcShelfThemeStyle"
    :class="{
      night: isNight,
      day: !isNight,
      'pc-shelf-theme': hasPcShelfTheme,
      'modern-shelf-layout': showModernShelf
    }"
  >
    <div
      class="navigation-wrapper"
      :class="[
        navigationClass,
        isWebApp && !isNight ? 'status-bar-light-bg' : ''
      ]"
      :style="navigationStyle"
      @touchstart="handleTouchStart"
      @touchmove="handleTouchMove"
      @touchend="handleTouchEnd"
      v-if="$store.getters.isNormalPage"
    >
      <div class="navigation-inner-wrapper">
        <div class="modern-nav" v-if="showModernShelf">
          <div class="modern-brand">
            <div class="modern-brand-mark">阅</div>
            <div>
              <h1>阅读</h1>
              <p>个人书架中心</p>
            </div>
          </div>
          <div class="modern-nav-section">
            <div class="modern-nav-title">书架</div>
            <button
              class="modern-nav-item"
              :class="{
                active: !isSearchResult && !isRankingView && !isHotSearchView
              }"
              @click="backToShelf"
            >
              <span>▦</span>
              <em>全部书籍</em>
              <strong>{{ shelfBooks.length }}</strong>
            </button>
            <button
              class="modern-nav-item"
              :class="{ disabled: !modernCurrentBook.bookUrl }"
              @click="toDetail(modernCurrentBook)"
            >
              <span>◷</span>
              <em>最近阅读</em>
              <strong>{{ modernCurrentBook.bookUrl ? 1 : 0 }}</strong>
            </button>
            <button class="modern-nav-item" @click="backToShelf">
              <span>↥</span>
              <em>追更更新</em>
              <strong>{{ modernUnreadBookCount }}</strong>
            </button>
            <button class="modern-nav-item" @click="showBookGroup = -2">
              <span>▣</span>
              <em>本地书籍</em>
              <strong>{{ getShowShelfBooks(-2).length }}</strong>
            </button>
            <button class="modern-nav-item" @click="showBookGroup = -3">
              <span>◉</span>
              <em>音频</em>
              <strong>{{ getShowShelfBooks(-3).length }}</strong>
            </button>
          </div>
          <div class="modern-nav-section">
            <div class="modern-nav-title">发现</div>
            <button
              class="modern-nav-item"
              :class="{ active: isRankingView }"
              @click="showRanking"
            >
              <span>☆</span>
              <em>排行榜</em>
              <strong></strong>
            </button>
            <button
              class="modern-nav-item"
              :class="{ active: isHotSearchView }"
              @click="showHotSearch"
            >
              <span>♨</span>
              <em>热搜</em>
              <strong></strong>
            </button>
          </div>
          <div class="modern-nav-section">
            <div class="modern-nav-title">管理</div>
            <button
              class="modern-nav-item"
              @click="showBookSourceManageDialog = true"
            >
              <span>⌘</span>
              <em>书源管理</em>
              <strong>{{ bookSourceList.length }}</strong>
            </button>
            <el-popover
              placement="right"
              width="360"
              trigger="click"
              :visible-arrow="false"
              popper-class="modern-action-popover"
            >
              <div class="modern-action-panel">
                <div class="modern-action-panel-head">
                  <strong>系统设置</strong>
                  <span>集中管理书架、书源、账户和缓存</span>
                </div>
                <div class="modern-action-section">
                  <div class="modern-action-title">书源</div>
                  <div class="modern-action-grid">
                    <button @click="showExplorePop">探索书源</button>
                    <button @click="uploadBookSource">导入书源</button>
                    <button @click="showBookSourceSubscriptionDialog">
                      书源订阅
                    </button>
                    <button @click="showFailureBookSource()">健康中心</button>
                    <button @click="debugBookSource()">调试书源</button>
                  </div>
                </div>
                <div class="modern-action-section">
                  <div class="modern-action-title">书架</div>
                  <div class="modern-action-grid">
                    <button @click="showBookManage">书籍管理</button>
                    <button @click="showManageBookGroup">分组管理</button>
                    <button @click="importLocalBook">导入书籍</button>
                    <button
                      v-if="
                        !$store.state.isSecureMode ||
                          $store.state.userInfo.enableLocalStore
                      "
                      @click="showLocalStoreManageDialog = true"
                    >
                      浏览书仓
                    </button>
                    <button @click="refreshShelf">刷新书架</button>
                    <button @click="showRssDialog">RSS</button>
                  </div>
                </div>
                <div class="modern-action-section">
                  <div class="modern-action-title">账户与同步</div>
                  <div class="modern-action-grid">
                    <button @click="showWebDAVManageDialog = true">
                      WebDAV
                    </button>
                    <button @click="backupToWebdav">保存备份</button>
                    <button
                      v-if="
                        $store.state.isSecureMode &&
                          $store.state.userInfo.username
                      "
                      @click="logout()"
                    >
                      注销
                    </button>
                    <button v-else @click="$store.commit('setShowLogin', true)">
                      登录
                    </button>
                    <button
                      v-if="localStorageAvaliable"
                      @click="saveUserConfig"
                    >
                      备份配置
                    </button>
                    <button
                      v-if="localStorageAvaliable"
                      @click="restoreUserConfig"
                    >
                      同步配置
                    </button>
                    <button
                      v-if="$store.state.showManagerMode"
                      @click="loadUserList"
                    >
                      加载空间
                    </button>
                    <button
                      v-if="$store.state.isManagerMode"
                      @click="showUserManageDialog()"
                    >
                      用户空间
                    </button>
                    <button
                      v-if="$store.state.isManagerMode"
                      @click="exitSecureMode"
                    >
                      退出管理
                    </button>
                  </div>
                </div>
                <div class="modern-action-section">
                  <div class="modern-action-title">系统</div>
                  <div class="modern-action-grid">
                    <button @click="showAiSettings">AI 设置</button>
                    <button @click="setIP">后端设置</button>
                    <button @click="init(true)">刷新缓存</button>
                    <button @click="clearCache('bookSourceList')">
                      清书源缓存
                    </button>
                    <button @click="clearCache('chapterList')">
                      清章节缓存
                    </button>
                    <button @click="showMPCode">公众号</button>
                    <button @click="joinTGChannel">TG频道</button>
                  </div>
                </div>
              </div>
              <button class="modern-nav-item" slot="reference">
                <span>⚙</span>
                <em>系统设置</em>
                <strong></strong>
              </button>
            </el-popover>
          </div>
          <div class="modern-server-card">
            <strong>{{ connectStatus }}</strong>
            <span :class="{ online: $store.state.connected }"></span>
            <button @click="setIP">后端设置</button>
          </div>
          <input
            ref="fileRef"
            type="file"
            @change="onSourceFileChange"
            style="display:none"
          />
          <input
            ref="bookRef"
            type="file"
            multiple="multiple"
            @change="onBookFileChange"
            style="display:none"
          />
        </div>
        <template v-else>
          <div class="navigation-title">
            阅读
            <span class="version-text" @click="updateForce">{{
              $store.state.version
            }}</span>
          </div>
          <div class="navigation-sub-title">
            清风不识字，何故乱翻书
          </div>
          <div class="search-wrapper">
            <div class="search-history-wrapper">
              <el-input
                size="mini"
                placeholder="搜索书籍"
                v-model="search"
                class="search-input"
                @keyup.enter.native="searchBook(1)"
                @focus="showSearchHistory = true"
                @blur="hideSearchHistory"
              >
                <i slot="prefix" class="el-input__icon el-icon-search"></i>
              </el-input>
              <div
                class="search-history-dropdown"
                v-if="showSearchHistory && searchHistory.length && !search"
              >
                <div class="search-history-header">
                  <span>搜索历史</span>
                  <span class="search-history-clear" @click="clearSearchHistory"
                    >清空</span
                  >
                </div>
                <div
                  class="search-history-item"
                  v-for="(item, index) in searchHistory"
                  :key="'sh2-' + index"
                  @mousedown.prevent="selectSearchHistory(item)"
                >
                  {{ item }}
                </div>
              </div>
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-connect"
                @click="showRanking"
              >
                排行榜
              </el-tag>
              <el-tag
                type="danger"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-connect"
                @click="showHotSearch"
              >
                热搜
              </el-tag>
            </div>
          </div>
          <div class="setting-wrapper search-setting">
            <div class="setting-title">
              搜索设置
            </div>
            <div class="setting-item">
              <el-select
                size="mini"
                v-model="searchConfig.searchType"
                class="setting-select"
                filterable
                placeholder="请选择搜索方式"
              >
                <el-option
                  v-for="(item, index) in searchTypeList"
                  :key="'search-type-' + index"
                  :label="item.name"
                  :value="item.value"
                >
                </el-option>
              </el-select>
            </div>
            <div
              class="setting-item"
              v-show="searchConfig.searchType === 'single'"
            >
              <el-select
                size="mini"
                v-model="searchConfig.bookSourceUrl"
                class="setting-select"
                filterable
                placeholder="请选择搜索书源"
              >
                <el-option
                  v-for="(item, index) in bookSourceList"
                  :key="'source-' + index"
                  :label="item.bookSourceName"
                  :value="item.bookSourceUrl"
                >
                </el-option>
              </el-select>
            </div>
            <div
              class="setting-item"
              v-show="searchConfig.searchType !== 'single'"
            >
              <el-select
                size="mini"
                v-model="searchConfig.bookSourceGroup"
                class="setting-select"
                filterable
                placeholder="请选择搜索书源分组"
              >
                <el-option
                  v-for="(item, index) in bookSourceGroupList"
                  :key="'source-group-' + index"
                  :label="item.name + ' (' + item.count + ')'"
                  :value="item.value"
                >
                </el-option>
              </el-select>
            </div>
            <div
              class="setting-item"
              v-show="searchConfig.searchType !== 'single'"
            >
              <el-select
                size="mini"
                v-model="searchConfig.concurrentCount"
                class="setting-select"
                filterable
                placeholder="请选择并发线程"
              >
                <el-option
                  v-for="(item, index) in concurrentList"
                  :key="'source-' + index"
                  :label="item + '并发线程'"
                  :value="item"
                >
                </el-option>
              </el-select>
            </div>
          </div>
          <div class="recent-wrapper">
            <div class="recent-title">
              最近阅读
            </div>
            <div class="reading-recent">
              <el-tag
                type="warning"
                :effect="isNight ? 'dark' : 'light'"
                class="recent-book"
                @click="toDetail(readingRecent)"
                :class="{ 'no-point': readingRecent.bookUrl == '' }"
              >
                {{ readingRecent.name }}
              </el-tag>
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-title">
              后端设定
            </div>
            <div class="setting-item">
              <el-tag
                :type="connectType"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-connect"
                :class="{ 'no-point': connecting }"
                @click="setIP"
              >
                {{ connectStatus }}
              </el-tag>
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-title">
              AI 设置
            </div>
            <div class="setting-item">
              <el-tag
                type="primary"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showAiSettings"
              >
                配置全本 AI
              </el-tag>
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-title">
              书源设置
            </div>
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showBookSourceManageDialog = true"
              >
                书源管理
              </el-tag>
              <el-popover
                placement="right"
                :width="popupWidth"
                trigger="click"
                :visible-arrow="false"
                v-model="popExploreVisible"
                popper-class="popper-component"
              >
                <Explore
                  ref="popExplore"
                  class="popup"
                  :visible="popExploreVisible"
                  :bookSourceList="bookSourceList"
                  @showSearchList="showSearchList"
                  @close="popExploreVisible = false"
                />
                <el-tag
                  type="info"
                  :effect="isNight ? 'dark' : 'light'"
                  slot="reference"
                  ref="exploreBtn"
                  class="setting-btn"
                  @click="showNavigation = false"
                >
                  探索书源
                </el-tag>
              </el-popover>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="uploadBookSource"
              >
                导入书源
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showBookSourceSubscriptionDialog"
              >
                书源订阅
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showFailureBookSource()"
              >
                健康中心
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="debugBookSource()"
              >
                调试书源
              </el-tag>
              <input
                ref="fileRef"
                type="file"
                @change="onSourceFileChange"
                style="display:none"
              />
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-title">
              书架设置
            </div>
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showBookManage"
              >
                书籍管理
              </el-tag>
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showManageBookGroup"
              >
                分组管理
              </el-tag>
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="importLocalBook"
              >
                导入书籍
              </el-tag>
              <input
                ref="bookRef"
                type="file"
                multiple="multiple"
                @change="onBookFileChange"
                style="display:none"
              />
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showLocalStoreManageDialog = true"
                v-if="
                  !$store.state.isSecureMode ||
                    $store.state.userInfo.enableLocalStore
                "
              >
                浏览书仓
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="init(true)"
              >
                刷新缓存
              </el-tag>
            </div>
          </div>

          <div class="setting-wrapper">
            <div class="setting-title">
              用户空间
              <span
                class="right-text"
                v-if="
                  $store.state.isSecureMode && $store.state.userInfo.username
                "
                @click="logout()"
                >注销</span
              >
              <span
                class="right-text"
                v-else
                @click="$store.commit('setShowLogin', true)"
                >登录</span
              >
            </div>
            <div class="setting-item" v-if="$store.state.showManagerMode">
              <el-select
                size="mini"
                v-model="userNS"
                class="setting-select"
                filterable
                placeholder="请选择用户空间"
              >
                <el-option
                  v-for="(item, index) in userList"
                  :key="'source-' + index"
                  :label="item.username"
                  :value="item.userNS"
                >
                </el-option>
              </el-select>
            </div>
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="saveUserConfig"
                v-if="localStorageAvaliable"
              >
                备份用户配置
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="restoreUserConfig"
                v-if="localStorageAvaliable"
              >
                同步用户配置
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="loadUserList"
                v-if="$store.state.showManagerMode"
              >
                加载用户空间
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                v-if="$store.state.isManagerMode"
                @click="showUserManageDialog()"
              >
                管理用户空间
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                v-if="$store.state.isManagerMode"
                @click="exitSecureMode"
              >
                退出管理模式
              </el-tag>
            </div>
          </div>
          <div
            class="setting-wrapper"
            v-if="
              !$store.state.isSecureMode || $store.state.userInfo.enableWebdav
            "
          >
            <div class="setting-title">
              WebDAV
            </div>
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showWebDAVManageDialog = true"
              >
                文件管理
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="backupToWebdav"
              >
                保存备份
              </el-tag>
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-title">
              其它
            </div>
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="showMPCode"
              >
                关注公众号【假装大佬】
              </el-tag>
              <el-tag
                type="info"
                :effect="isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="joinTGChannel"
              >
                加入TG频道【假装大佬】
              </el-tag>
            </div>
          </div>
          <div class="setting-wrapper">
            <div class="setting-title">
              本地缓存
              <span class="right-text">{{ localCacheStats.total }}</span>
            </div>
            <div class="setting-item">
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="clearCache('bookSourceList')"
              >
                清空书源缓存
                <span>{{ localCacheStats.bookSourceList }}</span>
              </el-tag>
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="clearCache('rssSources')"
              >
                清空RSS源缓存
                <span>{{ localCacheStats.rssSources }}</span>
              </el-tag>
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="clearCache('chapterList')"
              >
                清空章节列表缓存
                <span>{{ localCacheStats.chapterList }}</span>
              </el-tag>
              <el-tag
                type="info"
                :effect="$store.getters.isNight ? 'dark' : 'light'"
                class="setting-btn"
                @click="clearCache('chapterContent')"
              >
                清空章节内容缓存
                <span>{{ localCacheStats.chapterContent }}</span>
              </el-tag>
            </div>
          </div>
        </template>
      </div>
      <div class="bottom-icons" v-if="!showModernShelf">
        <a href="https://github.com/hectorqin/reader" target="_blank">
          <div class="bottom-icon">
            <img
              v-if="isNight"
              :src="require('../assets/imgs/github.png')"
              alt=""
            />
            <img v-else :src="require('../assets/imgs/github2.png')" alt="" />
          </div>
        </a>
        <span
          class="theme-item"
          :style="themeColor"
          ref="themes"
          @click="toogleNight"
        >
          <i class="el-icon-moon" v-if="!isNight"></i>
          <i class="el-icon-sunny" v-else></i>
        </span>
      </div>
    </div>
    <div
      class="shelf-wrapper"
      :class="[
        isWebApp && !isNight ? 'status-bar-light-bg' : '',
        { 'modern-shelf': showModernShelf }
      ]"
      ref="shelfWrapper"
      @click="showNavigation = false"
      @scroll="scrollHandler"
    >
      <div class="modern-topbar" v-if="showModernShelf">
        <div class="modern-search-controls">
          <el-select
            size="small"
            v-model="searchConfig.bookSourceGroup"
            class="modern-source-group-select"
            filterable
            placeholder="全部分组"
          >
            <el-option
              v-for="(item, index) in bookSourceGroupList"
              :key="'modern-source-group-' + index"
              :label="item.name + ' (' + item.count + ')'"
              :value="item.value"
            >
            </el-option>
          </el-select>
          <div class="search-history-wrapper">
            <el-input
              size="small"
              placeholder="搜索书名、作者、章节、书源"
              v-model="search"
              class="modern-search-input"
              @keyup.enter.native="searchBook(1)"
              @focus="showSearchHistory = true"
              @blur="hideSearchHistory"
            >
              <i slot="prefix" class="el-input__icon el-icon-search"></i>
            </el-input>
            <div
              class="search-history-dropdown"
              v-if="showSearchHistory && searchHistory.length && !search"
            >
              <div class="search-history-header">
                <span>搜索历史</span>
                <span class="search-history-clear" @click="clearSearchHistory"
                  >清空</span
                >
              </div>
              <div
                class="search-history-item"
                v-for="(item, index) in searchHistory"
                :key="'sh-' + index"
                @mousedown.prevent="selectSearchHistory(item)"
              >
                {{ item }}
              </div>
            </div>
          </div>
        </div>
        <div class="modern-top-actions">
          <button
            class="modern-icon-btn"
            title="刷新"
            @click.stop="refreshShelf"
          >
            <i class="el-icon-refresh-right"></i>
          </button>
          <button
            class="modern-icon-btn"
            title="导入书籍"
            @click.stop="importLocalBook"
          >
            <i class="el-icon-plus"></i>
          </button>
          <button
            class="modern-text-btn primary"
            :class="{ disabled: !modernCurrentBook.bookUrl }"
            :disabled="!modernCurrentBook.bookUrl"
            @click.stop="
              modernCurrentBook.bookUrl && toDetail(modernCurrentBook)
            "
          >
            继续阅读
          </button>
        </div>
      </div>
      <div
        class="modern-overview"
        v-if="
          showModernShelf &&
            !isSearchResult &&
            !isRankingView &&
            !isHotSearchView
        "
      >
        <div
          class="modern-continue-card"
          :class="{ disabled: !modernCurrentBook.bookUrl }"
          @click.stop="modernCurrentBook.bookUrl && toDetail(modernCurrentBook)"
        >
          <div class="modern-continue-cover">
            <el-image
              :class="{ 'empty-cover': !modernCurrentBook.bookUrl }"
              class="cover"
              :src="getCover(getBookCoverUrl(modernCurrentBook), true)"
              fit="cover"
              lazy
            ></el-image>
          </div>
          <div class="modern-continue-info">
            <div>
              <div class="modern-eyebrow">正在阅读</div>
              <h2>{{ modernCurrentBook.name || "尚无阅读记录" }}</h2>
              <p>
                {{
                  modernCurrentBook.durChapterTitle ||
                    modernCurrentBook.latestChapterTitle ||
                    "从书架中选择一本书开始阅读"
                }}
              </p>
            </div>
            <div class="modern-meta-row">
              <span>{{ modernCurrentBook.author || "未知作者" }}</span>
              <span v-if="modernCurrentBook.totalChapterNum"
                >共{{ modernCurrentBook.totalChapterNum }}章</span
              >
              <span v-if="modernUnreadCount(modernCurrentBook)"
                >未读{{ modernUnreadCount(modernCurrentBook) }}章</span
              >
            </div>
            <div class="modern-progress-row" v-if="modernCurrentBook.bookUrl">
              <div class="modern-progress-track">
                <i
                  :style="{
                    width: modernBookProgress(modernCurrentBook) + '%'
                  }"
                ></i>
              </div>
              <span>{{ modernBookProgress(modernCurrentBook) }}%</span>
            </div>
            <div class="modern-hero-actions">
              <button
                class="modern-text-btn primary"
                :class="{ disabled: !modernCurrentBook.bookUrl }"
                :disabled="!modernCurrentBook.bookUrl"
                @click.stop="
                  modernCurrentBook.bookUrl && toDetail(modernCurrentBook)
                "
              >
                打开阅读
              </button>
              <button
                class="modern-text-btn"
                :class="{ disabled: !modernCurrentBook.bookUrl }"
                :disabled="!modernCurrentBook.bookUrl"
                @click.stop="modernOpenReaderPanel('catalog')"
              >
                查看目录
              </button>
              <button
                class="modern-text-btn"
                :class="{ disabled: !modernCurrentBook.bookUrl }"
                :disabled="!modernCurrentBook.bookUrl"
                @click.stop="modernOpenReaderPanel('source')"
              >
                换源
              </button>
            </div>
          </div>
        </div>
        <div class="modern-update-card">
          <div class="modern-panel-head">
            <div>
              <h3>今日更新</h3>
              <p>优先处理有未读章节的书籍</p>
            </div>
            <span>{{ modernUnreadBookCount }} 本</span>
          </div>
          <div class="modern-update-list">
            <div
              class="modern-update-item"
              v-for="book in modernUpdatedBooks"
              :key="'modern-update-' + book.bookUrl"
              @click.stop="toDetail(book)"
            >
              <el-image
                class="modern-mini-cover"
                :src="getCover(getBookCoverUrl(book), true)"
                fit="cover"
                lazy
              ></el-image>
              <div class="modern-update-info">
                <strong>{{ book.name }}</strong>
                <span>{{
                  book.latestChapterTitle || book.durChapterTitle
                }}</span>
              </div>
              <em>{{ modernUnreadCount(book) }}</em>
            </div>
            <div class="modern-empty-tip" v-if="!modernUpdatedBooks.length">
              暂无未读更新
            </div>
          </div>
          <div class="modern-stat-grid">
            <div>
              <strong>{{ shelfBooks.length }}</strong>
              <span>藏书</span>
            </div>
            <div>
              <strong>{{ modernUnreadChapterCount }}</strong>
              <span>未读章节</span>
            </div>
            <div>
              <strong>{{ bookSourceList.length }}</strong>
              <span>书源</span>
            </div>
          </div>
        </div>
      </div>
      <div class="main-card shelf-main-card" :style="shelfMainCardStyle">
        <div class="shelf-title">
          <i
            class="el-icon-menu"
            v-if="$store.getters.isNormalPage && collapseMenu"
            @click.stop="toggleMenu"
          ></i>
          {{
            isHotSearchView
              ? "热搜"
              : isRankingView
              ? "排行榜"
              : isSearchResult
              ? isExploreResult
                ? "探索"
                : "搜索"
              : "书架"
          }}
          <span v-if="!isRankingView && !isHotSearchView"
            >({{ bookList.length }})</span
          >
          <div
            class="title-btn"
            v-if="$store.getters.isNormalPage && isSearchResult"
            @click="backToShelf"
          >
            书架
          </div>
          <div
            class="title-btn"
            v-if="
              $store.getters.isNormalPage && (isRankingView || isHotSearchView)
            "
            @click="backToShelf"
          >
            书架
          </div>
          <div
            class="title-btn"
            v-if="$store.getters.isNormalPage && isSearchResult"
            @click="loadMore"
          >
            <i class="el-icon-loading" v-if="loadingMore"></i>
            {{ loadingMore ? "加载中..." : "加载更多" }}
          </div>
          <div
            class="title-btn"
            v-if="
              $store.getters.isNormalPage &&
                !isSearchResult &&
                !isRankingView &&
                !isHotSearchView
            "
            @click="showBookEditButton = !showBookEditButton"
          >
            {{ showBookEditButton ? "取消" : "编辑" }}
          </div>
          <div
            class="title-btn"
            v-if="!isSearchResult && !isRankingView && !isHotSearchView"
            @click="refreshShelf"
          >
            <i class="el-icon-loading" v-if="refreshLoading"></i>
            {{ refreshLoading ? "刷新中..." : "刷新" }}
          </div>
          <div
            class="title-btn"
            v-if="
              $store.getters.isNormalPage &&
                !isSearchResult &&
                !isRankingView &&
                !isHotSearchView
            "
            @click="showRssDialog"
          >
            RSS
          </div>
          <el-popover
            v-if="
              showModernShelf &&
                $store.getters.isNormalPage &&
                !(isSearchResult && !isExploreResult) &&
                !isRankingView &&
                !isHotSearchView
            "
            placement="bottom-end"
            :width="popupWidth"
            trigger="click"
            :visible-arrow="false"
            v-model="popExploreVisible"
            popper-class="popper-component"
          >
            <Explore
              ref="popExplore"
              class="popup"
              :visible="popExploreVisible"
              :bookSourceList="bookSourceList"
              @showSearchList="showSearchList"
              @close="popExploreVisible = false"
            />
            <div class="title-btn" slot="reference" ref="exploreBtn">
              书海
            </div>
          </el-popover>
          <div
            class="title-btn"
            @click="showExplorePop"
            v-else-if="
              $store.getters.isNormalPage &&
                !(isSearchResult && !isExploreResult) &&
                !isRankingView &&
                !isHotSearchView
            "
          >
            书海
          </div>
        </div>
        <div
          class="book-group-wrapper"
          v-if="!isSearchResult && !isRankingView && !isHotSearchView"
        >
          <el-tabs
            class="book-group-tabs"
            v-model="showBookGroupString"
            stretch
          >
            <el-tab-pane
              v-for="group in bookGroupDisplayList"
              :label="group.groupName"
              :name="'' + group.groupId"
              :key="'bookGroup-' + group.groupId"
            ></el-tab-pane>
          </el-tabs>
        </div>
        <div
          class="books-wrapper"
          v-show="!isRankingView && !isHotSearchView"
          ref="bookList"
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
          @scroll="scrollHandler"
        >
          <div class="wrapper">
            <div
              class="book"
              :style="
                showNavigation && !showModernShelf
                  ? { minWidth: '360px !important' }
                  : {}
              "
              v-for="book in bookList"
              :key="book.bookUrl"
              @click="toDetail(book)"
              @contextmenu="openBookContextMenu($event, book)"
            >
              <div class="cover-img" @click.stop="showBookInfoDialog(book)">
                <!-- <img class="cover" v-lazy="getCover(book.coverUrl)" alt="" /> -->
                <el-image
                  class="cover"
                  ref="bookCoverList"
                  :src="getCover(getBookCoverUrl(book), true)"
                  fit="cover"
                  lazy
                >
                </el-image>
              </div>
              <div class="info" @click="toDetail(book)">
                <div class="book-operation">
                  <i
                    class="el-icon-close"
                    v-if="!isSearchResult && showBookEditButton"
                    @click.stop="deleteBook(book)"
                  ></i>
                  <i
                    class="el-icon-edit"
                    v-if="!isSearchResult && showBookEditButton"
                    @click.stop="editBook(book)"
                  ></i>
                  <i
                    class="el-icon-edit"
                    v-if="isSearchResult"
                    @click.stop="editBook(book, true)"
                  ></i>
                  <el-badge
                    class="unread-num-badge"
                    :max="99"
                    :value="book.totalChapterNum - 1 - book.durChapterIndex"
                    v-if="
                      !isSearchResult &&
                        !showBookEditButton &&
                        book.totalChapterNum - 1 - book.durChapterIndex > 0
                    "
                  />
                </div>
                <div
                  class="name"
                  slot="reference"
                  :class="showBookEditButton ? 'edit' : ''"
                >
                  {{ book.name }}
                </div>
                <div class="sub">
                  <div class="author">
                    {{ book.author || "" }}
                  </div>
                  <div class="dot" v-if="book.totalChapterNum">•</div>
                  <div class="size" v-if="book.totalChapterNum">
                    共{{ book.totalChapterNum }}章
                  </div>
                </div>
                <div
                  class="dur-chapter"
                  v-if="!isSearchResult && book.durChapterTitle"
                >
                  已读：{{ book.durChapterTitle }}
                </div>
                <div class="last-chapter" v-if="book.latestChapterTitle">
                  {{
                    book.lastCheckTime
                      ? dateFormat(book.lastCheckTime)
                      : "最新"
                  }}：{{ book.latestChapterTitle }}
                </div>
                <div
                  class="modern-book-progress"
                  v-if="
                    showModernShelf && !isSearchResult && book.totalChapterNum
                  "
                >
                  <span
                    ><i :style="{ width: modernBookProgress(book) + '%' }"></i
                  ></span>
                  <em>{{ modernBookProgress(book) }}%</em>
                </div>
                <div v-if="isSearchResult">
                  <el-tag
                    type="success"
                    :effect="isNight ? 'dark' : 'light'"
                    class="setting-connect"
                    @click.stop="addBookToShelf(book)"
                  >
                    加入书架
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
        <BookRanking
          ref="bookRanking"
          :visible="isRankingView"
          :api="api"
          @searchBook="searchFromRanking"
        />
        <HotSearch :visible="isHotSearchView" :api="api" />
      </div>
    </div>
    <div
      v-if="bookContextMenu.visible"
      class="book-context-menu"
      :style="{
        left: bookContextMenu.x + 'px',
        top: bookContextMenu.y + 'px'
      }"
      @click.stop
      @contextmenu.prevent.stop
    >
      <button
        type="button"
        class="book-context-menu-item danger"
        @click.stop="deleteBookFromContextMenu"
      >
        <i class="el-icon-delete"></i>
        <span>删除书籍</span>
      </button>
      <button
        type="button"
        class="book-context-menu-item"
        @click.stop="setBookGroupFromContextMenu"
      >
        <i class="el-icon-folder"></i>
        <span>调整分组</span>
      </button>
    </div>
    <el-dialog
      :title="isImportRssSource ? '导入RSS源' : '导入书源'"
      :visible.sync="showImportSourceDialog"
      :width="dialogWidth"
      :top="this.collapseMenu ? '0' : '15vh'"
      :fullscreen="collapseMenu"
      :class="isWebApp && !isNight ? 'status-bar-light-bg' : ''"
      v-if="$store.getters.isNormalPage"
    >
      <div class="source-container source-list-container">
        <el-checkbox-group
          v-model="checkedSourceIndex"
          @change="handleCheckedSourcesChange"
        >
          <el-checkbox
            v-for="(source, index) in importSourceList"
            :label="index"
            :key="index"
            class="source-checkbox"
            >{{ isImportRssSource ? source.sourceName : source.bookSourceName }}
            {{ isImportRssSource ? source.sourceUrl : source.bookSourceUrl }}
            {{ getSourceTag(source) }}</el-checkbox
          >
        </el-checkbox-group>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-checkbox
          :indeterminate="isIndeterminate"
          v-model="checkAll"
          @change="handleCheckAllChange"
          border
          size="medium"
          class="float-left"
          >全选</el-checkbox
        >
        <span class="check-tip">已选择 {{ checkedSourceIndex.length }} 个</span>
        <el-button
          size="medium"
          @click="
            showImportSourceDialog = false;
            checkedSourceIndex = [];
          "
          >取消</el-button
        >
        <el-button size="medium" type="primary" @click="saveSourceList"
          >确定</el-button
        >
      </div>
    </el-dialog>
    <el-dialog
      title="书源订阅"
      :visible.sync="showBookSourceSubscriptionDialogVisible"
      :width="dialogWidth"
      :top="dialogTop"
      :fullscreen="collapseMenu"
      :class="isWebApp && !isNight ? 'status-bar-light-bg-dialog' : ''"
      v-if="$store.getters.isNormalPage"
    >
      <div class="source-container subscription-container">
        <el-form
          :inline="!collapseMenu"
          :model="bookSourceSubscriptionForm"
          size="small"
        >
          <el-form-item label="名称">
            <el-input
              v-model="bookSourceSubscriptionForm.name"
              placeholder="可选"
            ></el-input>
          </el-form-item>
          <el-form-item label="订阅链接" class="subscription-url-form-item">
            <el-input
              v-model="bookSourceSubscriptionForm.url"
              placeholder="请输入书源订阅链接"
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="bookSourceSubscriptionSaving"
              @click="saveBookSourceSubscription"
              >保存订阅</el-button
            >
          </el-form-item>
        </el-form>
        <el-table
          :data="bookSourceSubscriptions"
          :height="dialogContentHeight - 42 - 60"
          v-loading="bookSourceSubscriptionLoading"
        >
          <el-table-column
            property="name"
            label="名称"
            min-width="120"
          ></el-table-column>
          <el-table-column property="url" label="订阅链接" min-width="220">
            <template slot-scope="scope">
              <el-link type="primary" :href="scope.row.url" target="_blank">
                {{ scope.row.url }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="95">
            <template slot-scope="scope">
              <el-tag
                size="mini"
                :type="
                  scope.row.lastStatus === 'success'
                    ? 'success'
                    : scope.row.lastStatus === 'error'
                    ? 'danger'
                    : 'info'
                "
              >
                {{
                  scope.row.lastStatus === "success"
                    ? "成功"
                    : scope.row.lastStatus === "error"
                    ? "失败"
                    : "未更新"
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="书源数量" width="90">
            <template slot-scope="scope">
              {{ scope.row.lastSourceCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="最后更新" width="120">
            <template slot-scope="scope">
              {{
                scope.row.lastSyncAt ? dateFormat(scope.row.lastSyncAt) : "-"
              }}
            </template>
          </el-table-column>
          <el-table-column
            property="lastError"
            label="错误信息"
            min-width="160"
          >
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button
                type="text"
                :loading="updatingBookSourceSubscriptionUrl === scope.row.url"
                @click="updateBookSourceSubscription(scope.row)"
                >更新</el-button
              >
              <el-button
                type="text"
                @click="deleteBookSourceSubscription(scope.row)"
                >删除</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          size="medium"
          @click="showBookSourceSubscriptionDialogVisible = false"
          >关闭</el-button
        >
      </div>
    </el-dialog>
    <el-dialog
      :visible.sync="showBookSourceManageDialog"
      :width="dialogWidth"
      :top="dialogTop"
      @closed="
        isShowFailureBookSource = false;
        showSourceGroup = '';
      "
      :fullscreen="collapseMenu"
      :class="isWebApp && !isNight ? 'status-bar-light-bg-dialog' : ''"
      v-if="$store.getters.isNormalPage"
    >
      <div class="custom-dialog-title" slot="title">
        <span class="el-dialog__title"
          >{{ isShowFailureBookSource ? "书源健康中心" : "书源管理" }}
          <span
            v-if="!isShowFailureBookSource"
            class="float-right span-btn"
            @click="deleteAllBookSource()"
            >清空</span
          >
          <span
            v-if="!isShowFailureBookSource"
            class="float-right span-btn"
            @click="deleteBookSourceFile()"
            >恢复默认</span
          >
          <span
            v-if="!isShowFailureBookSource"
            class="float-right span-btn"
            @click="exportBookSource()"
            >导出</span
          >
          <span
            v-if="!isShowFailureBookSource"
            class="float-right span-btn"
            @click="editBookSource(false)"
            >新增</span
          >
        </span>
      </div>
      <div class="source-container table-container">
        <div class="check-form" v-if="isShowFailureBookSource">
          <span class="check-form-label">搜索词：</span>
          <el-input v-model="checkBookSourceConfig.keyword" size="small">
          </el-input>
          <span class="check-form-label" style="min-width: 68px;">
            超时(ms)：
          </span>
          <el-input-number
            v-model="checkBookSourceConfig.timeout"
            :min="1000"
            :max="15000"
            :step="500"
            size="small"
          >
          </el-input-number>
          <span class="check-form-label">并发数：</span>
          <el-input-number
            v-model="checkBookSourceConfig.concurrent"
            :min="3"
            :max="15"
            :step="1"
            size="small"
          >
          </el-input-number>
          <span class="health-summary" v-if="bookSourceHealthSummary">
            总数 {{ bookSourceHealthSummary.total || 0 }} / 正常
            {{ bookSourceHealthSummary.healthy || 0 }} / 异常
            {{ bookSourceHealthSummary.invalid || 0 }} / 禁用
            {{ bookSourceHealthSummary.disabled || 0 }} / 书架使用
            {{ bookSourceHealthSummary.used || 0 }}
          </span>
        </div>
        <div class="source-group-wrapper">
          <el-tag
            type="info"
            :effect="$store.getters.isNight ? 'dark' : 'light'"
            class="source-group-btn"
            :class="showSourceGroup === name ? 'selected' : ''"
            v-for="name in bookSourceShowGroup"
            :key="'sourceGroup-' + name"
            @click="setShowSourceGroup(name)"
          >
            {{ name }}
          </el-tag>
        </div>
        <el-table
          :data="bookSourceShowResultPageList"
          :height="
            dialogContentHeight - 42 - 42 - (isShowFailureBookSource ? 32 : 0)
          "
          @selection-change="manageSourceSelection = $event"
          :key="isShowFailureBookSource"
        >
          <el-table-column
            type="selection"
            width="25"
            :fixed="$store.state.miniInterface"
            :selectable="isBookSourceSelectable"
          >
          </el-table-column>
          <el-table-column
            property="bookSourceName"
            label="书源名称"
            min-width="120"
            :fixed="$store.state.miniInterface"
          ></el-table-column>
          <el-table-column
            property="bookSourceUrl"
            label="书源链接"
            min-width="120"
          >
            <template slot-scope="scope">
              <el-link
                type="primary"
                :href="scope.row.bookSourceUrl"
                target="_blank"
                >{{ scope.row.bookSourceUrl }}</el-link
              >
            </template>
          </el-table-column>
          <el-table-column
            property="statusName"
            label="状态"
            min-width="80"
            v-if="isShowFailureBookSource"
          ></el-table-column>
          <el-table-column
            property="errorMsg"
            label="错误信息"
            min-width="120"
            v-if="isShowFailureBookSource"
          ></el-table-column>
          <el-table-column
            property="shelfBookCount"
            label="使用数"
            min-width="80"
            v-if="isShowFailureBookSource"
          ></el-table-column>
          <el-table-column label="书架书籍" min-width="120">
            <template slot-scope="scope">
              <pre>{{ showSourceBook(scope.row) }}</pre>
            </template>
          </el-table-column>
          <el-table-column
            label="操作"
            width="100px"
            v-if="!isShowFailureBookSource"
          >
            <template slot-scope="scope">
              <el-button type="text" @click="editBookSource(scope.row)"
                >编辑</el-button
              >
            </template>
          </el-table-column>
        </el-table>
        <div class="source-pagination">
          <el-pagination
            :current-page.sync="bookSourcePagination.page"
            :page-sizes="[25, 50, 100, 200, 300, 400]"
            :page-size.sync="bookSourcePagination.size"
            layout="total, sizes, prev, pager, next"
            :total="bookSourceShowLength"
            :pager-count="collapseMenu ? 5 : 7"
          >
          </el-pagination>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          class="float-left"
          size="medium"
          @click="deleteBookSourceList"
          >批量删除</el-button
        >
        <span class="check-tip"
          >已选择 {{ manageSourceSelection.length }} 个</span
        >
        <el-button
          @click="checkBookSource"
          v-if="isShowFailureBookSource"
          size="medium"
          style="margin-bottom: 5px;"
          :disabled="isCheckingBookSource"
          >{{ isCheckingBookSource ? "正在" : "" }}检测书源
          {{ checkBookSourceTip }}</el-button
        >
        <el-button @click="showBookSourceManageDialog = false" size="medium"
          >取消</el-button
        >
      </div>
    </el-dialog>

    <el-dialog
      :title="'导入本地书籍' + importMultiBookTip"
      :visible.sync="showImportBookDialog"
      :width="dialogSmallWidth"
      :top="dialogTop"
      @closed="importBookDialogClosed"
      :fullscreen="collapseMenu"
      :class="isWebApp && !isNight ? 'status-bar-light-bg-dialog' : ''"
      v-if="$store.getters.isNormalPage"
    >
      <div class="source-container table-container">
        <div class="check-form">
          <div class="book-cover">
            <el-image
              class="cover"
              :src="getCover(getBookCoverUrl(importBookInfo), true)"
              :key="getBookCoverUrl(importBookInfo)"
              fit="cover"
              lazy
            >
            </el-image>
          </div>
          <div class="book-info">
            <div>
              <span>书名：</span>
              <el-input v-model="importBookInfo.name" size="small"> </el-input>
            </div>
            <div>
              <span>作者：</span>
              <el-input v-model="importBookInfo.author" size="small">
              </el-input>
            </div>
            <div>
              <span>分组：</span>
              <el-select
                size="mini"
                v-model="importBookGroup"
                filterable
                multiple
                placeholder="未分组"
              >
                <el-option
                  v-for="(bookGroup, index) in bookGroupSetList"
                  :key="'bookGroup-' + index"
                  :label="bookGroup.groupName"
                  :value="bookGroup.groupId"
                >
                </el-option>
              </el-select>
            </div>
            <div v-if="isShowTocRule">
              <span>规则：</span>
              <el-select
                size="mini"
                v-model="importUsedTxtRule"
                filterable
                placeholder="内置规则"
              >
                <el-option
                  v-for="(rule, index) in tocRuleList"
                  :key="'txtTocRule-' + index"
                  :label="rule.name"
                  :value="rule.rule"
                >
                </el-option>
              </el-select>
              <el-button
                class="toc-refresh-btn"
                type="text"
                @click="getChapterListByRule()"
                >刷新目录</el-button
              >
            </div>
            <div v-if="isShowTocRule">
              <el-input
                type="textarea"
                :rows="2"
                v-model="importBookInfo.tocUrl"
                size="small"
              >
              </el-input>
            </div>
          </div>
        </div>
        <div class="chapter-title">
          章节列表({{ importBookChapters.length }})
        </div>
        <div
          class="chapter-list"
          :style="{ maxHeight: dialogContentHeight - 40 - 35 + 'px' }"
        >
          <p v-for="(chapter, index) in importBookChapters" :key="index">
            {{ index + 1 }}. {{ chapter.title }}
          </p>
        </div>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button
          type="primary"
          size="medium"
          @click="saveBook(importBookInfo, true)"
          >确定导入</el-button
        >
        <el-button size="medium" @click="showImportBookDialog = false"
          >取消</el-button
        >
      </div>
    </el-dialog>

    <LocalStore
      v-model="showLocalStoreManageDialog"
      @importFromLocalPathPreview="importMultiBooks"
    ></LocalStore>

    <WebDAV
      v-model="showWebDAVManageDialog"
      @importFromLocalPathPreview="importMultiBooks"
    ></WebDAV>
  </div>
</template>

<script>
import { mapGetters } from "vuex";
import Explore from "../components/Explore.vue";
import BookRanking from "../components/BookRanking.vue";
import HotSearch from "../components/HotSearch.vue";
import LocalStore from "../components/LocalStore.vue";
import WebDAV from "../components/WebDAV.vue";
import Axios from "../plugins/axios";
import { setCache, getCache } from "../plugins/cache";
import eventBus from "../plugins/eventBus";
import { formatSize, LimitResquest } from "../plugins/helper";
import { formatDate, getBytesLength } from "../plugins/utils";
const buildURL = require("axios/lib/helpers/buildURL");
import { isInContainer } from "element-ui/src/utils/dom";
import Vue from "vue";

export default {
  components: {
    Explore,
    LocalStore,
    WebDAV,
    BookRanking,
    HotSearch
  },
  data() {
    return {
      search: "",
      searchHistory: [],
      showSearchHistory: false,
      searchTypeList: [
        { name: "单源搜索", value: "single" },
        { name: "多源搜索(过滤书名/作者名)", value: "multi" }
      ],
      isSearchResult: false,
      isExploreResult: false,
      isRankingView: false,
      isHotSearchView: false,
      rankingScrollState: null,
      searchResult: [],
      searchPage: 1,
      refreshLoading: false,
      searchLastIndex: -1,

      showBookEditButton: false,
      bookContextMenu: {
        visible: false,
        x: 0,
        y: 0,
        book: null
      },

      popExploreVisible: false,
      loadingMore: false,

      importSourceList: [],
      showImportSourceDialog: false,
      isImportRssSource: false,
      checkAll: false,
      isIndeterminate: false,
      checkedSourceIndex: [],

      defaultBookSourceSubscriptionUrl:
        "https://shuyuan-api.yiove.com/redirect/shuyuan/20251014043613.json",
      showBookSourceSubscriptionDialogVisible: false,
      bookSourceSubscriptions: [],
      bookSourceSubscriptionForm: {
        name: "",
        url: ""
      },
      bookSourceSubscriptionLoading: false,
      bookSourceSubscriptionSaving: false,
      updatingBookSourceSubscriptionUrl: "",

      showBookSourceManageDialog: false,
      manageSourceSelection: [],
      isShowFailureBookSource: false,
      bookSourceHealthList: [],
      bookSourceHealthSummary: null,
      checkBookSourceTip: "",
      isCheckingBookSource: false,

      showNavigation: false,

      navigationClass: "",
      navigationStyle: {},

      popIntroVisible: {},

      connecting: false,

      lastScrollTop: 0,

      localStorageAvaliable:
        window.localStorage &&
        window.localStorage.getItem &&
        window.localStorage.setItem,

      showSourceGroup: "",
      bookSourcePagination: {
        page: 1,
        size: 25
      },
      checkBookSourceConfig: {
        keyword: "斗罗大陆",
        timeout: 5000,
        concurrent: 5
      },
      importBookInfo: {},
      importBookGroup: [],
      importBookChapters: [],
      showImportBookDialog: false,

      importMultiBookTip: "",

      rssSource: {},

      concurrentList: [12, 18, 24, 30, 36, 42, 48, 54, 60],

      localCacheStats: {
        total: "0 Bytes",
        bookSourceList: "0 Bytes",
        rssSources: "0 Bytes",
        chapterList: "0 Bytes",
        chapterContent: "0 Bytes"
      },

      showLocalStoreManageDialog: false,

      showWebDAVManageDialog: false,

      importUsedTxtRule: "",

      showAddUser: false,
      addUserForm: {
        username: "",
        password: ""
      }
    };
  },
  watch: {
    searchConfig: {
      handler(val) {
        this.$store.commit("setSearchConfig", val);
        if (this.isSearchResult) {
          this.searchBook(1);
        }
      },
      deep: true
    },
    searchResult(val) {
      if (this.isSearchResult && val.length) {
        this.$nextTick(() => {
          this.$refs.bookList.scrollTop = this.lastScrollTop;
        });
      }
    },
    collapseMenu(val) {
      if (!val) {
        this.navigationClass = "";
      } else if (!this.showNavigation) {
        this.navigationClass = "navigation-hidden";
      }
    },
    showNavigation(val) {
      if (!val) {
        this.navigationClass = "navigation-out";
        setTimeout(() => {
          this.navigationClass = "navigation-hidden";
        }, 300);
      } else {
        this.navigationClass = "navigation-in";
      }
    },
    loginAuth() {
      this.init(true);
    },
    userNS() {
      this.init(true);
    },
    importUsedTxtRule(val) {
      if (val) {
        this.importBookInfo.tocUrl = val;
      }
    },
    importBookGroup(val) {
      if (val && this.showImportBookDialog) {
        let groupId = 0;
        val.forEach(v => {
          groupId |= v;
        });
        this.importBookInfo.group = groupId;
      }
    },
    showBookGroup() {
      this.closeBookContextMenu();
      this.$nextTick(() => {
        // 手动处理 el-image 图片加载
        setTimeout(this.ensureLoadBookCover);
      });
    },
    isSearchResult() {
      this.closeBookContextMenu();
    }
  },
  mounted() {
    document.title = "阅读";
    this.navigationClass =
      this.collapseMenu && !this.showNavigation ? "navigation-hidden" : "";
    window.shelfPage = this;
    this.loadSearchHistory();
    this.init();
    eventBus.$on("onSourceFileChange", (event, isRssSource) => {
      if (this._inactive) {
        return;
      }
      this.onSourceFileChange(event, isRssSource);
    });
    eventBus.$on("editBook", (book, isAdd, onSuccess) => {
      if (this._inactive) {
        return;
      }
      this.editBook(book, isAdd, onSuccess);
    });
    document.addEventListener(
      "click",
      this.handleBookContextMenuDocumentClick,
      true
    );
    document.addEventListener("keydown", this.handleBookContextMenuKeydown);
  },
  activated() {
    document.title = "阅读";
    this.scanCacheStorage();
    if (this.$route.query.openAiSettings) {
      eventBus.$emit("showAiSettings");
      this.$router.replace({ path: "/" });
    }
  },
  deactivated() {
    this.closeBookContextMenu();
  },
  beforeDestroy() {
    document.removeEventListener(
      "click",
      this.handleBookContextMenuDocumentClick,
      true
    );
    document.removeEventListener("keydown", this.handleBookContextMenuKeydown);
  },
  methods: {
    init(refresh) {
      this.$root.$children[0].init(refresh);
    },
    showAiSettings() {
      eventBus.$emit("showAiSettings");
    },
    setIP() {
      this.$prompt("请输入接口地址 ( 如：localhost:8080/reader3 )", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        inputValue: this.api,
        // inputPattern: /^((2[0-4]\d|25[0-5]|[1]?\d\d?)\.){3}(2[0-4]\d|25[0-5]|[1]?\d\d?):([1-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]|[1-6][0-5][0-5][0-3][0-5])$/,
        // inputErrorMessage: "url 形式不正确",
        beforeClose: (action, instance, done) => {
          if (action === "confirm") {
            this.connecting = true;
            instance.confirmButtonLoading = true;
            instance.confirmButtonText = "校验中……";
            var inputUrl = instance.inputValue.replace(/\/*$/g, "");
            this.loadBookshelf(inputUrl)
              .then(() => {
                this.connecting = false;
                instance.confirmButtonLoading = false;
                done();
                setCache("api_prefix", inputUrl);
                this.$store.commit("setApi", inputUrl);
                // 初始化
                this.init();
              })
              .catch(() => {
                instance.confirmButtonLoading = false;
                instance.confirmButtonText = "确定";
              });
          } else {
            done();
          }
        }
      })
        .then(({ value }) => {
          this.$message({
            type: "success",
            message: "与" + value + "连接成功"
          });
        })
        .catch(() => {});
    },
    loadBookshelf(api, refresh) {
      api = api || this.api;
      if (!api) {
        this.$message.error("请先设置后端接口地址");
        this.$store.commit("setConnected", false);
        return Promise.reject(false);
      }

      if (!this.loading || !this.loading.visible) {
        this.loading = this.$loading({
          target: this.$refs.bookList,
          lock: true,
          text: refresh ? "正在刷新书籍信息" : "正在获取书籍信息",
          spinner: "el-icon-loading",
          background: this.isNight ? "#222" : "#fff"
        });
      }

      if (
        !api.startsWith("http://") &&
        !api.startsWith("https://") &&
        !api.startsWith("//")
      ) {
        api = "//" + api;
      }

      return this.$root.$children[0].loadBookShelf(refresh, api).then(() => {
        this.loading.close();
      });
    },
    refreshShelf() {
      return this.loadBookshelf(null, true);
    },
    loadBookGroup(refresh) {
      return this.$root.$children[0].loadBookGroup(refresh);
    },
    loadBookSource(refresh) {
      return this.$root.$children[0].loadBookSource(refresh);
    },
    normalizeSearchResultText(text) {
      return (text || "")
        .toString()
        .toLowerCase()
        .replace(
          /[\s!"#$%&'()*+,\-./:;<=>?@[\\\]^_`{|}~《》〈〉「」『』【】（）“”‘’·—–…，。！？；：、]/g,
          ""
        );
    },
    getSearchResultAuthorKey(author) {
      const authorKey = this.normalizeSearchResultText(author);
      return [
        "",
        "未知",
        "佚名",
        "匿名",
        "无",
        "暂无",
        "null",
        "unknown",
        "作者"
      ].indexOf(authorKey) >= 0
        ? ""
        : authorKey;
    },
    getSearchResultIdentityKey(book) {
      const nameKey = this.normalizeSearchResultText(book && book.name);
      const authorKey = this.getSearchResultAuthorKey(book && book.author);
      const originKey = this.normalizeSearchResultText(book && book.origin);
      const urlKey = this.normalizeSearchResultText(book && book.bookUrl);
      if (nameKey && authorKey) {
        return `book:${nameKey}:${authorKey}`;
      }
      const latestChapterKey = this.normalizeSearchResultText(
        book && book.latestChapterTitle
      );
      if (nameKey && latestChapterKey.length >= 3) {
        return `weak:${nameKey}:${latestChapterKey}`;
      }
      if (originKey && urlKey) {
        return `url:${originKey}:${urlKey}`;
      }
      if (originKey && nameKey) {
        return `source-name:${originKey}:${nameKey}`;
      }
      return `raw:${(book && book.origin) || ""}:${(book && book.bookUrl) ||
        ""}:${(book && book.name) || ""}:${(book && book.author) || ""}`;
    },
    getSearchResultCompletenessScore(book) {
      let score = 0;
      if (book && book.bookUrl) score += 10;
      if (book && book.origin) score += 5;
      if (this.getSearchResultAuthorKey(book && book.author)) score += 80;
      if (book && book.latestChapterTitle) score += 40;
      if (book && book.coverUrl) score += 25;
      if (book && book.intro) score += 20;
      if (book && book.wordCount) score += 20;
      if (book && book.tocUrl) score += 10;
      return score;
    },
    getSearchResultScore(book, keyword) {
      const keywordKey = this.normalizeSearchResultText(keyword || this.search);
      const nameKey = this.normalizeSearchResultText(book && book.name);
      const authorKey = this.getSearchResultAuthorKey(book && book.author);
      let score = this.getSearchResultCompletenessScore(book);
      if (keywordKey) {
        if (nameKey === keywordKey) {
          score += 1000;
        } else if (nameKey.indexOf(keywordKey) === 0) {
          score += 800;
        } else if (nameKey.indexOf(keywordKey) >= 0) {
          score += 650;
        } else if (nameKey.length >= 2 && keywordKey.indexOf(nameKey) >= 0) {
          score += 300;
        }
        if (authorKey === keywordKey) {
          score += 500;
        } else if (authorKey.indexOf(keywordKey) >= 0) {
          score += 350;
        }
        const latestChapterKey = this.normalizeSearchResultText(
          book && book.latestChapterTitle
        );
        if (latestChapterKey.indexOf(keywordKey) >= 0) score += 120;
        const introKey = this.normalizeSearchResultText(
          book && book.intro
        ).slice(0, 2000);
        if (introKey.indexOf(keywordKey) >= 0) score += 60;
        if (nameKey) {
          score += Math.max(
            0,
            80 - Math.abs(nameKey.length - keywordKey.length) * 4
          );
        }
      }
      score += Math.max(0, Math.min((book && book.originOrder) || 0, 200));
      if (book && book.time > 0) {
        score += Math.max(0, 80 - Math.min(Math.floor(book.time / 250), 80));
      }
      return score;
    },
    isBetterSearchResult(candidate, current, keyword) {
      const candidateScore = this.getSearchResultScore(candidate, keyword);
      const currentScore = this.getSearchResultScore(current, keyword);
      if (candidateScore !== currentScore) {
        return candidateScore > currentScore;
      }
      const candidateCompleteness = this.getSearchResultCompletenessScore(
        candidate
      );
      const currentCompleteness = this.getSearchResultCompletenessScore(
        current
      );
      if (candidateCompleteness !== currentCompleteness) {
        return candidateCompleteness > currentCompleteness;
      }
      if ((candidate.originOrder || 0) !== (current.originOrder || 0)) {
        return (candidate.originOrder || 0) > (current.originOrder || 0);
      }
      const candidateTime =
        candidate.time > 0 ? candidate.time : Number.MAX_VALUE;
      const currentTime = current.time > 0 ? current.time : Number.MAX_VALUE;
      if (candidateTime !== currentTime) {
        return candidateTime < currentTime;
      }
      return (candidate.bookUrl || "").length < (current.bookUrl || "").length;
    },
    compareSearchResults(left, right, keyword) {
      const scoreDiff =
        this.getSearchResultScore(right, keyword) -
        this.getSearchResultScore(left, keyword);
      if (scoreDiff) return scoreDiff;
      const orderDiff = (right.originOrder || 0) - (left.originOrder || 0);
      if (orderDiff) return orderDiff;
      const leftTime = left.time > 0 ? left.time : Number.MAX_VALUE;
      const rightTime = right.time > 0 ? right.time : Number.MAX_VALUE;
      if (leftTime !== rightTime) return leftTime - rightTime;
      const lengthDiff =
        this.normalizeSearchResultText(left.name).length -
        this.normalizeSearchResultText(right.name).length;
      if (lengthDiff) return lengthDiff;
      return (left.name || "").localeCompare(right.name || "");
    },
    mergeSearchResultList(baseList, incomingList, keyword) {
      const resultMap = {};
      let changed = false;
      const mergeOne = (book, markChanged) => {
        if (!book || (!book.name && !book.bookUrl)) return;
        const bookKey = this.getSearchResultIdentityKey(book);
        const current = resultMap[bookKey];
        if (!current) {
          resultMap[bookKey] = book;
          changed = changed || markChanged;
        } else if (this.isBetterSearchResult(book, current, keyword)) {
          resultMap[bookKey] = book;
          changed = changed || markChanged;
        }
      };
      (baseList || []).forEach(book => mergeOne(book, false));
      (incomingList || []).forEach(book => mergeOne(book, true));
      return {
        changed,
        list: Object.keys(resultMap)
          .map(key => resultMap[key])
          .sort((left, right) =>
            this.compareSearchResults(left, right, keyword || this.search)
          )
      };
    },
    loadSearchHistory() {
      try {
        const saved = localStorage.getItem("searchHistory");
        this.searchHistory = saved ? JSON.parse(saved) : [];
      } catch (e) {
        this.searchHistory = [];
      }
    },
    addSearchHistory(keyword) {
      if (!keyword || !keyword.trim()) return;
      keyword = keyword.trim();
      this.searchHistory = this.searchHistory.filter(k => k !== keyword);
      this.searchHistory.unshift(keyword);
      if (this.searchHistory.length > 20) {
        this.searchHistory = this.searchHistory.slice(0, 20);
      }
      localStorage.setItem("searchHistory", JSON.stringify(this.searchHistory));
    },
    clearSearchHistory() {
      this.searchHistory = [];
      localStorage.removeItem("searchHistory");
    },
    hideSearchHistory() {
      setTimeout(() => {
        this.showSearchHistory = false;
      }, 150);
    },
    selectSearchHistory(keyword) {
      this.search = keyword;
      this.showSearchHistory = false;
      this.searchBook(1);
    },
    searchBook(page) {
      if (!this.$store.state.connected) {
        this.$message.error("后端未连接");
        return;
      }
      if (!this.search) {
        this.$message.error("请输入关键词进行搜索");
        return;
      }
      if (
        this.searchConfig.searchType === "single" &&
        !this.searchConfig.bookSourceUrl
      ) {
        this.$message.error("请选择书源进行搜索");
        return;
      }
      if (page) {
        this.searchPage = page;
      }
      page = this.searchPage;
      this.addSearchHistory(this.search);
      if (page === 1) {
        // 重新搜索
        this.searchLastIndex = -1;
      }
      if (this.searchConfig.searchType === "multi" && window.EventSource) {
        this.searchBookByEventStream(page);
        return;
      }
      if (this.loadingMore) {
        return;
      }
      this.isRankingView = false;
      this.isHotSearchView = false;
      this.isSearchResult = true;
      this.isExploreResult = false;
      this.loadingMore = true;
      if (page === 1) {
        this.searchResult = [];
      }
      Axios.post(
        this.api +
          (this.searchConfig.searchType === "single"
            ? "/searchBook"
            : "/searchBookMulti"),
        {
          key: this.search,
          bookSourceUrl: this.searchConfig.bookSourceUrl,
          bookSourceGroup: this.searchConfig.bookSourceGroup,
          concurrentCount: this.searchConfig.concurrentCount,
          lastIndex: this.searchLastIndex, // 多源搜索时的索引
          page: page // 单源搜索时的page
        },
        {
          timeout: this.searchConfig.searchType === "single" ? 30000 : 180000
        }
      ).then(
        res => {
          this.loadingMore = false;
          if (res.data.isSuccess) {
            //
            let resultList = [];
            if (this.searchConfig.searchType === "single") {
              resultList = res.data.data;
            } else {
              this.searchLastIndex = res.data.data.lastIndex;
              resultList = res.data.data.list;
            }
            const merged = this.mergeSearchResultList(
              this.searchResult,
              resultList,
              this.search
            );
            this.searchResult = merged.list;
            if (!merged.changed) {
              this.$message.error("没有更多啦");
            }
          }
        },
        error => {
          this.$message.error("搜索书籍失败 " + (error && error.toString()));
        }
      );
    },
    searchBookByEventStream(page) {
      const tryClose = () => {
        try {
          if (
            this.searchEventSource &&
            this.searchEventSource.readyState != this.searchEventSource.CLOSED
          ) {
            this.searchEventSource.close();
          }
          this.searchEventSource = null;
        } catch (error) {
          //
        }
      };
      if (this.loadingMore) {
        tryClose();
        this.loadingMore = false;
        // page === 1 是重新搜索
        if (page !== 1) {
          // 停止搜索
          return;
        }
      }
      const params = {
        accessToken: this.$store.state.token,
        key: this.search,
        bookSourceUrl: this.searchConfig.bookSourceUrl,
        bookSourceGroup: this.searchConfig.bookSourceGroup,
        concurrentCount: this.searchConfig.concurrentCount,
        lastIndex: this.searchLastIndex, // 多源搜索时的索引
        page: page // 单源搜索时的page
      };

      this.isRankingView = false;
      this.isHotSearchView = false;
      this.isSearchResult = true;
      this.isExploreResult = false;
      this.loadingMore = true;
      if (page === 1) {
        this.searchResult = [];
      }
      const url = buildURL(this.api + "/searchBookMultiSSE", params);

      tryClose();

      this.searchEventSource = new EventSource(url, {
        withCredentials: true
      });
      this.searchEventSource.addEventListener("error", e => {
        this.loadingMore = false;
        tryClose();
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
      let oldSearchResultLength = this.searchResult.length;
      let hasSearchResultChange = false;
      this.searchEventSource.addEventListener("end", e => {
        this.loadingMore = false;
        tryClose();
        try {
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.lastIndex) {
              this.searchLastIndex = result.lastIndex;
            }
          }
          if (
            !hasSearchResultChange &&
            this.searchResult.length === oldSearchResultLength
          ) {
            this.$message.error("没有更多啦");
          }
        } catch (error) {
          //
        }
      });
      this.searchEventSource.addEventListener("message", e => {
        try {
          if (e.data) {
            const result = JSON.parse(e.data);
            if (result && result.lastIndex) {
              this.searchLastIndex = result.lastIndex;
            }
            if (result.data) {
              const merged = this.mergeSearchResultList(
                this.searchResult,
                result.data,
                this.search
              );
              hasSearchResultChange = hasSearchResultChange || merged.changed;
              this.searchResult = merged.list;
            }
          }
        } catch (error) {
          //
        }
      });
    },
    toDetail(book, panel) {
      if (!book.bookUrl) {
        return;
      }
      if (this.isSearchResult) {
        // this.$message.error("请先加入书架");
        // return;
      }
      this.$store.commit("setReadingBook", {
        name: book.name,
        bookUrl: book.bookUrl,
        index: book.index ?? book.durChapterIndex ?? 0,
        type: book.type,
        coverUrl: this.getBookCoverUrl(book),
        tocUrl: book.tocUrl,
        author: book.author,
        origin: book.origin,
        originName: book.originName,
        latestChapterTitle: book.latestChapterTitle,
        intro: book.intro,
        kind: book.kind,
        wordCount: book.wordCount,
        variable: book.variable,
        originOrder: book.originOrder
      });
      const query = {};
      if (this.isSearchResult) {
        query.search = "1";
      }
      if (panel) {
        query.panel = panel;
      }
      this.$router.push({
        path: "/reader",
        query
      });
    },
    modernOpenReaderPanel(panel) {
      if (!this.modernCurrentBook.bookUrl) {
        return;
      }
      this.toDetail(this.modernCurrentBook, panel);
    },
    async addBookToShelf(book) {
      const customImportBookInfo = await this.customImportBookInfo({
        title: "设置分组",
        cancelButtonText: "暂不加入"
      });
      if (customImportBookInfo === false) {
        return;
      }
      this.saveBook({ ...book, ...customImportBookInfo });
    },
    saveBook(book, isImport, isEdit) {
      if (!book || !book.bookUrl || !book.origin) {
        this.$message.error("书籍信息错误");
        return Promise.reject(false);
      }
      return Axios.post(this.api + "/saveBook", book).then(
        res => {
          if (res.data.isSuccess) {
            //
            if (isImport) {
              this.showImportBookDialog = false;
            }
            this.$message.success(
              isImport
                ? "导入书籍成功"
                : isEdit
                ? "修改书籍成功"
                : "加入书架成功"
            );
            if (!isEdit) {
              this.loadBookshelf();
            } else {
              this.$store.commit("updateShelfBook", res.data.data);
            }
            return res.data.data;
          }
        },
        error => {
          this.$message.error(
            (isImport
              ? "导入书籍失败"
              : isEdit
              ? "修改书籍失败"
              : "加入书架失败 ") + (error && error.toString())
          );
        }
      );
    },
    async deleteBook(book) {
      if (!book || (!book.name && !book.bookUrl)) {
        this.$message.error("书籍信息错误");
        return;
      }
      const res = await this.$confirm(
        "此操作将删除书籍信息以及阅读进度, 是否继续?",
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/deleteBook", book).then(
        res => {
          if (res.data.isSuccess) {
            //
            this.$message.success("删除成功");
            this.loadBookshelf();
          }
        },
        error => {
          this.$message.error("删除失败 " + (error && error.toString()));
        }
      );
    },
    editBook(book, isAdd, onSuccess) {
      if (!book || !book.name || !book.bookUrl || !book.origin) {
        this.$message.error("书籍信息错误");
        return;
      }
      const bookInfo = { ...book };
      delete bookInfo["variableMap$delegate"];
      eventBus.$emit(
        "showEditor",
        isAdd ? "保存书籍" : "编辑书籍",
        JSON.stringify(bookInfo, null, 4),
        async (content, close) => {
          try {
            const newBook = JSON.parse(content);
            if (!newBook.name) {
              this.$message.error("书籍名称不能为空");
              return;
            }
            if (!newBook.bookUrl) {
              this.$message.error("书籍链接不能为空");
              return;
            }
            if (!newBook.origin) {
              this.$message.error("书籍来源不能为空");
              return;
            }
            if (isAdd) {
              const res = await this.$confirm(
                "加入书架之后才能编辑书籍信息, 是否加入书架?",
                "提示",
                {
                  confirmButtonText: "确定",
                  cancelButtonText: "取消",
                  type: "warning"
                }
              ).catch(() => {
                return false;
              });
              if (!res) {
                return;
              }
            }
            this.saveBook(newBook, false, true).then(() => {
              close();
              if (onSuccess) {
                onSuccess();
              }
            });
          } catch (e) {
            this.$message.error("书籍信息必须是JSON格式");
          }
        }
      );
    },
    currentDateTime() {
      const now = new Date();
      const pad = a => (a < 10 ? "0" + a : a);
      return (
        now.getFullYear() +
        pad(now.getMonth() + 1) +
        pad(now.getDate()) +
        "_" +
        pad(now.getHours()) +
        pad(now.getMinutes()) +
        pad(now.getSeconds())
      );
    },
    dateFormat(t) {
      let time = new Date().getTime();
      let int = parseInt((time - t) / 1000);
      let str = "";

      if (int <= 30) {
        str = "刚刚";
      } else if (int < 60) {
        str = int + "秒前";
      } else if (int < 3600) {
        str = parseInt(int / 60) + "分钟前";
      } else if (int < 86400) {
        str = parseInt(int / 3600) + "小时前";
      } else if (int < 2592000) {
        str = parseInt(int / 86400) + "天前";
      } else if (int < 31536000) {
        str = parseInt(int / 2592000) + "月前";
      } else {
        str = parseInt(int / 31536000) + "年前";
      }
      return str;
    },
    showRanking() {
      this.isSearchResult = false;
      this.isExploreResult = false;
      this.isHotSearchView = false;
      this.isRankingView = true;
      this.showNavigation = false;
    },
    showHotSearch() {
      this.isSearchResult = false;
      this.isExploreResult = false;
      this.isRankingView = false;
      this.isHotSearchView = true;
      this.showNavigation = false;
    },
    // eslint-disable-next-line no-unused-vars
    searchFromRanking(name, author) {
      if (!this.$store.state.connected) {
        this.$message.error("后端未连接");
        return;
      }
      if (!this.bookSourceList.length) {
        this.$message.warning("请先导入书源后再搜索");
        return;
      }
      this.rankingScrollState = this.$refs.bookRanking
        ? this.$refs.bookRanking.getScrollState()
        : null;
      this.search = name;
      this.isRankingView = false;
      this.isHotSearchView = false;
      this.searchBook(1);
    },
    backToShelf() {
      if (this.isSearchResult && this.rankingScrollState) {
        this.isSearchResult = false;
        this.isExploreResult = false;
        this.searchResult = [];
        this.loadingMore = false;
        this.isRankingView = true;
        this.isHotSearchView = false;
        this.$nextTick(() => {
          if (this.$refs.bookRanking) {
            this.$refs.bookRanking.restoreScrollState(this.rankingScrollState);
          }
        });
        this.rankingScrollState = null;
        return;
      }
      this.isSearchResult = false;
      this.isExploreResult = false;
      this.isRankingView = false;
      this.isHotSearchView = false;
      this.searchResult = [];
      this.loadingMore = false;
    },
    toogleNight() {
      if (this.isNight) {
        this.$store.commit("setNightTheme", false);
      } else {
        this.$store.commit("setNightTheme", true);
      }
    },
    showSearchList(data) {
      this.isSearchResult = true;
      this.isExploreResult = true;
      this.isRankingView = false;
      this.isHotSearchView = false;
      this.loadingMore = false;
      this.searchResult = data;
    },
    loadMore() {
      this.lastScrollTop = this.$refs.bookList.scrollTop;
      if (this.isExploreResult) {
        this.loadingMore = true;
        this.$refs.popExplore.loadMore();
      } else {
        this.searchBook(this.searchPage + 1);
      }
    },
    uploadBookSource() {
      this.$refs.fileRef.dispatchEvent(new MouseEvent("click"));
    },
    onSourceFileChange(event, isRssSource) {
      const rawFile = event.target.files && event.target.files[0];
      // console.log("rawFile", rawFile);
      const reader = new FileReader();
      const sourceTypeName = isRssSource ? "RSS源" : "书源";
      reader.onload = e => {
        const data = e.target.result;
        try {
          const sourceList = JSON.parse(data);
          if (Array.isArray(sourceList) && sourceList.length) {
            this.importSourceList = sourceList.map(v => {
              if (v.headerMap) {
                if (!v.header) {
                  v.header =
                    typeof v.headerMap === "string"
                      ? v.headerMap
                      : JSON.stringify(v.headerMap);
                }
                delete v.headerMap;
              }
              return v;
            });
            this.showImportSourceDialog = true;
            this.isImportRssSource = !!isRssSource;
          } else {
            this.$message.error(sourceTypeName + "文件错误");
          }
        } catch (error) {
          this.$message.error(sourceTypeName + "文件错误");
        }
      };
      reader.onerror = () => {
        // console.log("FileReader error", e);
        // FileReader 读取出错，只能上传读取了
        let param = new FormData();
        param.append("file", rawFile);
        Axios.post(this.api + "/readSourceFile", param, {
          headers: { "Content-Type": "multipart/form-data" }
        }).then(
          res => {
            if (res.data.isSuccess) {
              //
              let sourceList = [];
              res.data.data.forEach(v => {
                try {
                  const data = JSON.parse(v);
                  if (Array.isArray(data)) {
                    sourceList = sourceList.concat(data);
                  }
                } catch (error) {
                  //
                }
              });
              if (sourceList.length) {
                this.importSourceList = sourceList.map(v => {
                  if (v.headerMap) {
                    if (!v.header) {
                      v.header =
                        typeof v.headerMap === "string"
                          ? v.headerMap
                          : JSON.stringify(v.headerMap);
                    }
                    delete v.headerMap;
                  }
                  return v;
                });
                this.showImportSourceDialog = true;
                this.isImportRssSource = !!isRssSource;
              } else {
                this.$message.error(sourceTypeName + "文件错误");
              }
            }
          },
          error => {
            this.$message.error(
              "读取" +
                sourceTypeName +
                "文件内容失败 " +
                (error && error.toString())
            );
          }
        );
      };
      reader.readAsText(rawFile);
      if (this.isRssSource) {
        this.$refs.rssInputRef.value = null;
      } else {
        this.$refs.fileRef.value = null;
      }
    },
    async loadRemoteBookSource() {
      const lastRemoteSourceUrl = getCache(
        this.currentUserName + "@lastRemoteSourceUrl",
        ""
      );
      const res = await this.$prompt("请输入远程书源链接", "导入远程书源文件", {
        inputValue: lastRemoteSourceUrl || "",
        confirmButtonText: "确定",
        cancelButtonText: "取消"
      }).catch(() => {
        return false;
      });
      if (!res || !res.value) {
        return;
      }
      Axios.post(this.api + "/readRemoteSourceFile", {
        url: res.value
      }).then(
        res => {
          if (res.data.isSuccess) {
            setCache(this.currentUserName + "@lastRemoteSourceUrl", res.value);
            //
            let sourceList = [];
            res.data.data.forEach(v => {
              try {
                const data = JSON.parse(v);
                if (Array.isArray(data)) {
                  sourceList = sourceList.concat(data);
                }
              } catch (error) {
                //
              }
            });
            if (sourceList.length) {
              this.importSourceList = sourceList;
              this.showImportSourceDialog = true;
              this.isImportRssSource = false;
            } else {
              this.$message.error("远程书源文件错误");
            }
          }
        },
        error => {
          this.$message.error(
            "读取远程书源文件内容失败 " + (error && error.toString())
          );
        }
      );
    },
    showBookSourceSubscriptionDialog() {
      this.showBookSourceSubscriptionDialogVisible = true;
      if (!this.bookSourceSubscriptionForm.url) {
        this.bookSourceSubscriptionForm.url = this.defaultBookSourceSubscriptionUrl;
      }
      this.loadBookSourceSubscriptions();
    },
    loadBookSourceSubscriptions() {
      this.bookSourceSubscriptionLoading = true;
      Axios.post(this.api + "/getBookSourceSubscriptions").then(
        res => {
          this.bookSourceSubscriptionLoading = false;
          if (res.data.isSuccess) {
            this.bookSourceSubscriptions = res.data.data || [];
          }
        },
        error => {
          this.bookSourceSubscriptionLoading = false;
          this.$message.error(
            "加载书源订阅失败 " + (error && error.toString())
          );
        }
      );
    },
    saveBookSourceSubscription() {
      const url = (this.bookSourceSubscriptionForm.url || "").trim();
      if (!url) {
        this.$message.error("请输入书源订阅链接");
        return;
      }
      this.bookSourceSubscriptionSaving = true;
      Axios.post(this.api + "/saveBookSourceSubscription", {
        name: this.bookSourceSubscriptionForm.name,
        url
      }).then(
        res => {
          this.bookSourceSubscriptionSaving = false;
          if (res.data.isSuccess) {
            this.$message.success("保存书源订阅成功");
            this.bookSourceSubscriptionForm = {
              name: "",
              url: this.defaultBookSourceSubscriptionUrl
            };
            this.loadBookSourceSubscriptions();
          }
          if (!res.data.isSuccess) {
            this.loadBookSourceSubscriptions();
          }
        },
        error => {
          this.bookSourceSubscriptionSaving = false;
          this.$message.error(
            "保存书源订阅失败 " + (error && error.toString())
          );
        }
      );
    },
    updateBookSourceSubscription(subscription) {
      if (!subscription || !subscription.url) {
        return;
      }
      this.updatingBookSourceSubscriptionUrl = subscription.url;
      Axios.post(this.api + "/updateBookSourceSubscription", {
        url: subscription.url
      }).then(
        res => {
          this.updatingBookSourceSubscriptionUrl = "";
          if (res.data.isSuccess) {
            this.$message.success(
              "更新书源订阅成功，导入 " +
                (res.data.data.importCount || 0) +
                " 个书源"
            );
            this.loadBookSourceSubscriptions();
            this.loadBookSource(true);
          }
          if (!res.data.isSuccess) {
            this.loadBookSourceSubscriptions();
          }
        },
        error => {
          this.updatingBookSourceSubscriptionUrl = "";
          const message =
            (error &&
              error.response &&
              error.response.data &&
              error.response.data.errorMsg) ||
            (error && error.toString());
          this.$message.error("更新书源订阅失败 " + message);
          this.loadBookSourceSubscriptions();
        }
      );
    },
    async deleteBookSourceSubscription(subscription) {
      if (!subscription || !subscription.url) {
        return;
      }
      const res = await this.$confirm(`确认要删除该书源订阅吗?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/deleteBookSourceSubscription", {
        url: subscription.url
      }).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success("删除书源订阅成功");
            this.loadBookSourceSubscriptions();
          }
          if (!res.data.isSuccess) {
            this.loadBookSourceSubscriptions();
          }
        },
        error => {
          this.$message.error(
            "删除书源订阅失败 " + (error && error.toString())
          );
        }
      );
    },
    handleCheckAllChange(val) {
      let hasFilterd = false;
      this.checkedSourceIndex = val
        ? this.importSourceList
            .map((v, i) => {
              // 不勾选使用了 js，webview的书源
              const source = JSON.stringify(v);
              if (
                source.indexOf("@js:") !== -1 ||
                source.indexOf("webView:") !== -1
              ) {
                hasFilterd = true;
                return false;
              }
              return i;
            })
            .filter(v => v)
        : [];
      if (val && hasFilterd) {
        this.$message.info("部分使用了Javascript和Webview的书源未勾选");
      }
      this.isIndeterminate = false;
    },
    handleCheckedSourcesChange(value) {
      let checkedCount = value.length;
      this.checkAll = checkedCount === this.importSourceList.length;
      this.isIndeterminate =
        checkedCount > 0 && checkedCount < this.importSourceList.length;
    },
    getSourceTag(source) {
      const sourceStr = JSON.stringify(source);
      const tags = [];
      if (sourceStr.indexOf("@js:") !== -1) {
        tags.push("@Javascript");
      }

      if (sourceStr.indexOf("webView:") !== -1) {
        tags.push("@WebView");
      }

      return "   " + tags.join("  ");
    },
    saveSourceList() {
      if (!this.$store.state.connected) {
        this.$message.error("后端未连接");
        return;
      }
      if (!this.checkedSourceIndex.length) {
        this.$message.error("请选择需要导入的源");
        return;
      }
      const sourceList = this.checkedSourceIndex.map(
        v => this.importSourceList[v]
      );
      Axios.post(
        this.api +
          (this.isImportRssSource ? "/saveRssSources" : "/saveBookSources"),
        sourceList
      ).then(
        res => {
          if (res.data.isSuccess) {
            //
            this.$message.success(
              this.isImportRssSource ? "导入RSS源成功" : "导入书源成功"
            );
            if (this.isImportRssSource) {
              this.loadRssSources(true);
            } else {
              this.loadBookSource(true);
            }
            this.showImportSourceDialog = false;
            this.isImportRssSource = false;
            this.checkedSourceIndex = [];
          }
        },
        error => {
          this.$message.error(
            (this.isImportRssSource ? "导入RSS源失败 " : "导入书源失败 ") +
              (error && error.toString())
          );
        }
      );
    },
    isBookSourceSelectable(bookSource) {
      if (this.isShowFailureBookSource) {
        return !bookSource.isUnknown && !(bookSource.shelfBookCount || 0);
      }
      const res = [];
      (this.$store.state.shelfBooks || []).forEach(v => {
        if (v.origin === bookSource.bookSourceUrl) {
          res.push(v.name);
        }
      });
      return !res.length;
    },
    showSourceBook(bookSource) {
      if (Array.isArray(bookSource.shelfBooks)) {
        return bookSource.shelfBooks.join("\n");
      }
      const res = [];
      (this.$store.state.shelfBooks || []).forEach(v => {
        if (v.origin === bookSource.bookSourceUrl) {
          res.push(v.name);
        }
      });
      return res.join("\n");
    },
    getInvalidBookSources() {
      if (!this.$store.state.connected) {
        this.$message.error("后端未连接");
        return;
      }
      Axios.post(this.api + "/getBookSourceHealth").then(
        res => {
          if (res.data.isSuccess) {
            const data = res.data.data || {};
            this.bookSourceHealthSummary = data.summary || null;
            this.bookSourceHealthList = data.list || [];
          }
        },
        error => {
          this.$message.error(
            "加载书源健康信息失败 " + (error && error.toString())
          );
        }
      );
    },
    async checkBookSource() {
      if (!this.checkBookSourceConfig.keyword) {
        this.$message.error("请输入搜索关键词");
        return;
      }
      this.isCheckingBookSource = true;
      this.$store.commit("setFailureIncludeTimeout", true);
      const limitFunc = LimitResquest(
        this.checkBookSourceConfig.concurrent,
        handler => {
          this.checkBookSourceTip =
            handler.requestCount + "/" + this.bookSourceList.length;
          if (handler.isEnd()) {
            this.isCheckingBookSource = false;
            this.$store.commit("setFailureIncludeTimeout", false);
            this.getInvalidBookSources();
          }
        }
      );
      this.bookSourceList.forEach(v => {
        limitFunc(() => {
          return Axios.post(
            this.api + "/searchBook",
            {
              key: this.checkBookSourceConfig.keyword,
              bookSourceUrl: v.bookSourceUrl
            },
            {
              timeout: this.checkBookSourceConfig.timeout,
              silent: true
            }
          );
        });
      });
    },
    async deleteBookSourceList() {
      if (!this.manageSourceSelection.length) {
        this.$message.error("请选择需要删除的源");
        return;
      }
      const res = await this.$confirm("确认要删除所选择的书源吗?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(
        this.api + "/deleteBookSources",
        this.manageSourceSelection
      ).then(
        res => {
          if (res.data.isSuccess) {
            this.$store.commit(
              "removeFailureBookSource",
              this.manageSourceSelection
            );
            this.manageSourceSelection = [];
            this.$message.success("删除书源成功");
            this.loadBookSource(true);
            if (this.isShowFailureBookSource) {
              this.getInvalidBookSources();
            }
          }
        },
        error => {
          this.$message.error("删除书源失败 " + (error && error.toString()));
        }
      );
    },
    toggleMenu() {
      if (this.collapseMenu) {
        this.showNavigation = !this.showNavigation;
      }
    },
    showExplorePop() {
      setTimeout(() => {
        this.popExploreVisible = true;
      }, 100);
    },
    showBookInfoDialog(book) {
      eventBus.$emit("showBookInfoDialog", book);
    },
    openBookContextMenu(event, book) {
      if (this.isSearchResult || !book || !book.bookUrl) {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      const menuWidth = 156;
      const menuHeight = 84;
      const edgePadding = 8;
      const viewportWidth =
        window.innerWidth || document.documentElement.clientWidth || menuWidth;
      const viewportHeight =
        window.innerHeight ||
        document.documentElement.clientHeight ||
        menuHeight;
      const maxX = Math.max(
        edgePadding,
        viewportWidth - menuWidth - edgePadding
      );
      const maxY = Math.max(
        edgePadding,
        viewportHeight - menuHeight - edgePadding
      );
      this.bookContextMenu = {
        visible: true,
        x: Math.min(Math.max(event.clientX, edgePadding), maxX),
        y: Math.min(Math.max(event.clientY, edgePadding), maxY),
        book
      };
    },
    closeBookContextMenu() {
      if (!this.bookContextMenu.visible) {
        return;
      }
      this.bookContextMenu = {
        visible: false,
        x: 0,
        y: 0,
        book: null
      };
    },
    handleBookContextMenuKeydown(event) {
      if (event.key === "Escape") {
        this.closeBookContextMenu();
      }
    },
    handleBookContextMenuDocumentClick(event) {
      if (!this.bookContextMenu.visible) {
        return;
      }
      if (
        event.target &&
        event.target.closest &&
        event.target.closest(".book-context-menu")
      ) {
        return;
      }
      this.closeBookContextMenu();
    },
    deleteBookFromContextMenu() {
      const book = this.bookContextMenu.book;
      this.closeBookContextMenu();
      this.deleteBook(book);
    },
    setBookGroupFromContextMenu() {
      const book = this.bookContextMenu.book;
      this.closeBookContextMenu();
      if (!book || !book.bookUrl) {
        this.$message.error("书籍信息错误");
        return;
      }
      this.$store.commit("setShowBookInfo", book);
      eventBus.$emit("showBookGroupDialog", true);
    },
    async saveUserConfig() {
      if (!window.localStorage) {
        this.$message.error("当前终端不支持localStorage");
        return;
      }
      const res = await this.$confirm(
        "确认要备份当前终端的阅读配置、书架设置、搜索设置、自定义配置方案吗?",
        "提示"
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      const userConfig = {};
      ["config", "shelfConfig", "searchConfig", "customConfigList"].forEach(
        key => {
          const val = getCache(key);
          if (val) {
            userConfig[key] = val;
          }
        }
      );
      Axios.post(this.api + "/saveUserConfig", userConfig).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success("备份成功");
          }
        },
        error => {
          this.$message.error("备份失败 " + (error && error.toString()));
        }
      );
    },
    async restoreUserConfig() {
      if (!window.localStorage) {
        this.$message.error("当前终端不支持localStorage");
        return;
      }
      const res = await this.$confirm(
        "确认要从备份文件中恢复当前终端的阅读配置、书架设置、搜索设置、自定义配置方案吗?",
        "提示"
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.get(this.api + "/getUserConfig").then(
        res => {
          if (res.data.isSuccess) {
            for (const key in res.data.data) {
              if (Object.hasOwnProperty.call(res.data.data, key)) {
                setCache(key, res.data.data[key]);
              }
            }
            this.$store.dispatch("syncFromLocalStorage");
            this.$message.success("恢复成功");
          }
        },
        error => {
          this.$message.error("恢复失败 " + (error && error.toString()));
        }
      );
    },
    loadUserList() {
      if (!this.$store.state.connected) {
        this.$message.error("后端未连接");
        return;
      }
      Axios.get(this.api + "/getUserList").then(
        res => {
          if (res.data.isSuccess) {
            this.userNS = this.$store.state.userInfo.username;
            this.userList = res.data.data.map(v => ({
              ...v,
              userNS: v.username
            }));
            this.$store.commit("setIsManagerMode", true);
          }
        },
        error => {
          this.$message.error(
            "加载用户空间失败 " + (error && error.toString())
          );
        }
      );
    },
    formatTableField(row, column, cellValue) {
      switch (column.property) {
        case "createdAt":
        case "lastLoginAt":
        case "lastModified":
          return cellValue
            ? formatDate(new Date(cellValue), "yy-MM-dd hh:mm")
            : "";
        case "size":
          return row.isDirectory ? "" : formatSize(cellValue);
        default:
          return cellValue;
      }
    },
    exitSecureMode() {
      this.userNS = "default";
      this.userList = [];
      this.$store.commit("setIsManagerMode", false);
      this.init(true);
    },
    async backupToWebdav() {
      const res = await this.$confirm(
        `确认要用当前Reader数据覆盖阅读App备份文件中的书源、书源订阅、书架、分组、RSS、替换规则、书签、用户配置和阅读配置吗?`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/backupToWebdav").then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success("备份成功");
          }
        },
        error => {
          this.$message.error("备份失败 " + (error && error.toString()));
        }
      );
    },
    handleTouchStart(e) {
      this.lastTouch = false;
      this.lastMoveX = false;
      this.touchMoveTimes = 0;
      // 边缘 20px 以内禁止触摸
      if (
        e.touches &&
        e.touches[0] &&
        e.touches[0].clientX > 20 &&
        e.touches[0].clientX < window.innerWidth - 20 &&
        e.touches[0].clientY > 20 &&
        e.touches[0].clientY < window.innerHeight - 20
      ) {
        this.lastTouch = e.touches[0];
      }
    },
    handleTouchMove(e) {
      if (e.touches && e.touches[0] && this.lastTouch) {
        if (this.collapseMenu) {
          const moveX = e.touches[0].clientX - this.lastTouch.clientX;
          const moveY = e.touches[0].clientY - this.lastTouch.clientY;
          if (Math.abs(moveY) > Math.abs(moveX)) {
            this.navigationStyle = {};
            this.lastMoveX = 0;
            return;
          }
          e.preventDefault();
          e.stopPropagation();
          if (!this.showNavigation && moveX > 0 && moveX <= 270) {
            // 往右拉，打开目录
            if (this.touchMoveTimes % 3 === 0) {
              this.navigationStyle = {
                marginLeft: moveX - 270 + "px"
              };
            }
            this.lastMoveX = moveX;
          } else if (this.showNavigation && moveX < 0 && moveX >= -270) {
            // 往左拉，关闭目录
            if (this.touchMoveTimes % 3 === 0) {
              this.navigationStyle = {
                marginLeft: moveX + "px"
              };
            }
            this.lastMoveX = moveX;
          }
          this.touchMoveTimes++;
        }
      }
    },
    handleTouchEnd() {
      if (this.collapseMenu) {
        if (this.lastMoveX > 0) {
          this.showNavigation = true;
          this.navigationStyle = {};
        } else if (this.lastMoveX < 0) {
          this.showNavigation = false;
          this.navigationStyle = {};
        }
      }
    },
    showFailureBookSource() {
      this.showSourceGroup = "";
      this.bookSourcePagination.page = 1;
      this.bookSourceHealthSummary = null;
      this.bookSourceHealthList = [];
      this.getInvalidBookSources();
      this.isShowFailureBookSource = true;
      this.showBookSourceManageDialog = true;
    },
    debugBookSource() {
      window.open(
        window.location.origin +
          window.location.pathname +
          "bookSourceDebug/#domain=" +
          this.api,
        "_target"
      );
    },
    setShowSourceGroup(group) {
      if (this.showSourceGroup === group) {
        this.showSourceGroup = "";
      } else {
        this.showSourceGroup = group;
      }
    },
    importLocalBook() {
      this.$refs.bookRef.dispatchEvent(new MouseEvent("click"));
    },
    onBookFileChange(event) {
      if (!event.target || !event.target.files || !event.target.files.length) {
        return;
      }
      let param = new FormData();
      for (let i = 0; i < event.target.files.length; i++) {
        const file = event.target.files[i];
        param.append("file" + i, file);
      }
      Axios.post(this.api + "/importBookPreview", param, {
        headers: { "Content-Type": "multipart/form-data" }
      }).then(
        res => {
          if (res.data.isSuccess && res.data.data.length) {
            if (res.data.data.length > 1) {
              // 批量导入
              this.importMultiBooks(res.data.data);
            } else {
              //
              this.importBookInfo = res.data.data[0].book;
              this.importBookGroup = [];
              this.importBookChapters = res.data.data[0].chapters;
              this.showImportBookDialog = true;
            }
          }
        },
        error => {
          this.$message.error("上传书籍 " + (error && error.toString()));
        }
      );
      this.$refs.bookRef.value = null;
    },
    async importMultiBooks(books) {
      if (!books || !books.length) {
        return;
      }
      if (books.length == 1) {
        this.importBookInfo = books[0].book;
        this.importBookGroup = [];
        this.importBookChapters = books[0].chapters;
        this.showImportBookDialog = true;
        return;
      }
      const res = await this.$confirm(
        `你选择导入多本书籍，请选择导入方式?`,
        "提示",
        {
          confirmButtonText: "批量导入",
          cancelButtonText: "逐一确认导入",
          type: "warning",
          closeOnClickModal: false,
          closeOnPressEscape: false,
          distinguishCancelAndClose: true
        }
      ).catch(action => {
        return action === "close" ? "close" : false;
      });
      if (res === "close") {
        return;
      }
      if (res) {
        const customImportBookInfo = await this.customImportBookInfo();
        if (customImportBookInfo === false) {
          return;
        }
        for (let i = 0; i < books.length; i++) {
          const book = books[i];
          await this.saveBook(
            { ...book.book, ...customImportBookInfo },
            true
          ).catch(() => {});
        }
      } else {
        for (let i = 0; i < books.length; i++) {
          const book = books[i];
          this.importMultiBookTip = `（${i + 1}/${books.length}）`;
          await this.waitForImportBook(book);
        }
        this.importMultiBookTip = "";
      }
    },
    waitForImportBook(bookInfo) {
      return new Promise(resolve => {
        this.importBookInfo = bookInfo.book;
        this.importBookGroup = [];
        this.importBookChapters = bookInfo.chapters;
        this.showImportBookDialog = true;
        this.$once("importEnd", resolve);
      });
    },
    importBookDialogClosed() {
      const url = this.importBookInfo.bookUrl;
      this.importBookInfo = {};
      this.importBookGroup = [];
      this.importBookChapters = [];
      this.importUsedTxtRule = "";
      this.$nextTick(() => {
        this.$emit("importEnd");
      });

      Axios.post(
        this.api + "/deleteFile",
        {
          url
        },
        {
          silent: true
        }
      ).then(
        () => {
          //
        },
        () => {
          //
        }
      );
    },
    async customImportBookInfo(options) {
      this.importBookGroup = [];
      const res = await this.$msgbox({
        title: "统一设置分组",
        message: this.renderComp(),
        showCancelButton: true,
        confirmButtonText: "确定",
        cancelButtonText: "取消导入",
        ...(options || {})
      }).catch(action => {
        return action === "close" ? "close" : false;
      });
      if (res === "confirm") {
        return {
          group: this.importBookGroup.reduce((v, c) => v | c, 0)
        };
      } else {
        return false;
      }
    },
    renderComp() {
      var bookGroupList = this.bookGroupSetList;
      var shelf = this;
      Vue.component("custComp", {
        render() {
          return (
            <div style={{ textAlign: "center" }}>
              <span>请选择分组：</span>
              <el-select
                size="mini"
                vModel={this.importBookGroup}
                ref="bookGroupSelect"
                filterable={true}
                multiple={true}
                placeholder="未分组"
                vOn:change={this.change}
              >
                {bookGroupList.map((bookGroup, index) => {
                  return (
                    <el-option
                      key={"bookGroup-" + index}
                      label={bookGroup.groupName}
                      value={bookGroup.groupId}
                    ></el-option>
                  );
                })}
              </el-select>
            </div>
          );
        },
        data() {
          return {
            importBookGroup: []
          };
        },
        methods: {
          change() {
            shelf.importBookGroup = this.importBookGroup;
          }
        }
      });
      var custComp = Vue.component("custComp");
      return this.$createElement(custComp);
    },
    showBookManage() {
      eventBus.$emit("showBookManageDialog");
    },
    showManageBookGroup() {
      this.loadBookGroup(true);
      eventBus.$emit("showBookGroupDialog", false);
    },
    getShowShelfBooks(bookGroup) {
      // 处理特殊分组
      if (bookGroup === -1) {
        // 全部
        return this.shelfBooks;
      } else if (bookGroup === -2) {
        // 本地
        return this.shelfBooks.filter(v => v.origin === "loc_book");
      } else if (bookGroup === -3) {
        // 音频
        return this.shelfBooks.filter(v => v.type === 1);
      } else if (bookGroup === -4) {
        // 未分组
        return this.shelfBooks.filter(v => v.group === 0);
      }

      return this.shelfBooks.filter(v =>
        bookGroup === 0 ? true : v.group & bookGroup
      );
    },
    loadRssSources(refresh) {
      return this.$root.$children[0].loadRssSources(refresh);
    },
    showRssDialog() {
      eventBus.$emit("showRssSourceListDialog");
    },
    showRssArticleListDialog(source) {
      eventBus.$emit("showRssArticleListDialog", source);
    },
    noop() {},
    exportBookSource() {
      Axios.get(this.api + "/getBookSources").then(
        res => {
          if (res.data.isSuccess) {
            const aEle = document.createElement("a");
            const blob = new Blob([
              JSON.stringify(res.data.data || [], null, 4)
            ]);

            aEle.download = "reader书源-" + this.currentDateTime() + ".json";
            aEle.href = URL.createObjectURL(blob);
            aEle.click();
          }
        },
        error => {
          this.$message.error("导出书源失败 " + (error && error.toString()));
        }
      );
    },
    async deleteAllBookSource() {
      const res = await this.$confirm(`确认要清空所有书源吗?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/deleteAllBookSources").then(
        res => {
          if (res.data.isSuccess) {
            //
            this.$message.success("清空书源成功");
            this.loadBookSource(true);
          }
        },
        error => {
          this.$message.error("清空书源失败 " + (error && error.toString()));
        }
      );
    },
    async deleteBookSourceFile() {
      const res = await this.$confirm(`确认要恢复默认书源吗?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).catch(() => {
        return false;
      });
      if (!res) {
        return;
      }
      Axios.post(this.api + "/deleteBookSourcesFile").then(
        res => {
          if (res.data.isSuccess) {
            //
            this.$message.success("恢复默认书源成功");
            this.loadBookSource(true);
          }
        },
        error => {
          this.$message.error("操作失败 " + (error && error.toString()));
        }
      );
    },
    editBookSource(bookSource) {
      const editHandler = data => {
        eventBus.$emit(
          "showEditor",
          "编辑书源",
          JSON.stringify(data, null, 4),
          (content, close) => {
            try {
              const source = JSON.parse(content);
              if (!source.bookSourceName) {
                this.$message.error("书源名称不能为空");
                return;
              }
              if (!source.bookSourceUrl) {
                this.$message.error("书源链接不能为空");
                return;
              }
              Axios.post(this.api + "/saveBookSource", source).then(
                res => {
                  if (res.data.isSuccess) {
                    //
                    close();
                    this.$message.success("保存书源成功");
                    this.loadBookSource(true);
                  }
                },
                error => {
                  this.$message.error(
                    "保存书源失败 " + (error && error.toString())
                  );
                }
              );
            } catch (e) {
              this.$message.error("书源必须是JSON格式");
            }
          }
        );
      };
      if (!bookSource) {
        editHandler({
          bookSourceComment: "",
          bookSourceGroup: "",
          bookSourceName: "新增书源",
          bookSourceType: 0,
          bookSourceUrl: "",
          bookUrlPattern: "",
          enabled: true,
          enabledExplore: true,
          exploreUrl: "",
          ruleBookInfo: {},
          ruleContent: {
            content: ""
          },
          ruleExplore: {},
          ruleSearch: {
            author: "",
            bookList: "",
            bookUrl: "",
            coverUrl: "",
            intro: "",
            kind: "",
            lastChapter: "",
            name: ""
          },
          ruleToc: {
            chapterList: "",
            chapterName: "",
            chapterUrl: ""
          },
          searchUrl: ""
        });
        return;
      }
      Axios.post(this.api + "/getBookSource", {
        bookSourceUrl: bookSource.bookSourceUrl
      }).then(
        res => {
          if (res.data.isSuccess) {
            //
            editHandler(res.data.data);
          }
        },
        error => {
          this.$message.error(
            "加载书源信息失败 " + (error && error.toString())
          );
        }
      );
    },
    updateForce() {
      if ("serviceWorker" in navigator) {
        navigator.serviceWorker
          .getRegistrations()
          .then(async function(registrations) {
            /* eslint-disable-next-line no-console */
            console.log("registrations", registrations);
            for (let i = 0; i < registrations.length; i++) {
              await registrations[i].update();
            }

            /* eslint-disable-next-line no-console */
            console.log("Try to clear home cache");
            navigator.serviceWorker.controller &&
              navigator.serviceWorker.controller.postMessage({
                type: "CLEAR_HOME_CACHE"
              });

            /* eslint-disable-next-line no-console */
            console.log("Try to skip waiting");
            navigator.serviceWorker.controller &&
              navigator.serviceWorker.controller.postMessage({
                type: "SKIP_WAITING"
              });

            setTimeout(() => {
              /* eslint-disable-next-line no-console */
              console.log("Try to reload force");
              window.location.reload(true);
            }, 50);
          });
      }
    },
    async scanCacheStorage() {
      this.localCacheStats = {
        total: (await this.analyseLocalStorage()).totalBytes,
        bookSourceList: (await this.analyseLocalStorage("bookSourceList"))
          .totalBytes,
        rssSources: (await this.analyseLocalStorage("rssSources")).totalBytes,
        chapterList: (await this.analyseLocalStorage("chapterList")).totalBytes,
        chapterContent: (await this.analyseLocalStorage("chapterContent"))
          .totalBytes
      };
    },
    analyseLocalStorage(match) {
      let totalBytes = 0;
      let cacheBytes = 0;
      return window.$cacheStorage
        .iterate(function(value, key) {
          if (!match || key.indexOf(match) >= 0) {
            totalBytes += getBytesLength(JSON.stringify(value));
            if (key.startsWith("localCache@")) {
              cacheBytes += getBytesLength(JSON.stringify(value));
            }
          }
        })
        .then(() => {
          return {
            totalBytes: formatSize(totalBytes),
            cacheBytes: formatSize(cacheBytes)
          };
        })
        .catch(function() {
          // 当出错时，此处代码运行
          // console.log(err);
        });
    },
    clearCache(match) {
      let cacheBytes = 0;
      window.$cacheStorage
        .iterate(function(value, key) {
          if (!match || key.indexOf(match) >= 0) {
            if (key.startsWith("localCache@")) {
              cacheBytes += getBytesLength(JSON.stringify(value));
              window.$cacheStorage.removeItem(key);
            }
          }
        })
        .then(() => {
          this.scanCacheStorage();

          return {
            cacheBytes: formatSize(cacheBytes)
          };
        })
        .catch(function() {
          // 当出错时，此处代码运行
          // console.log(err);
        });
    },
    scrollHandler(event) {
      const target =
        (event && event.target) ||
        this.$refs.bookList ||
        this.$refs.shelfWrapper;
      this.lastScrollTop = target.scrollTop || 0;
      this.closeBookContextMenu();
    },
    modernUnreadCount(book) {
      if (!book || !book.totalChapterNum) return 0;
      const index = book.durChapterIndex ?? book.index ?? 0;
      return Math.max((book.totalChapterNum || 0) - 1 - index, 0);
    },
    modernBookProgress(book) {
      if (!book || !book.totalChapterNum) return 0;
      const index = book.durChapterIndex ?? book.index ?? 0;
      return Math.min(
        100,
        Math.max(0, Math.round(((index + 1) * 100) / book.totalChapterNum))
      );
    },
    getBookCoverUrl(book) {
      if (!book) return "";
      return book.customCoverUrl || book.coverUrl;
    },
    logout() {
      Axios.post(this.api + "/logout").then(
        res => {
          if (res.data.isSuccess) {
            this.$store.commit("setToken", "");
            window.location.reload(true);
          }
        },
        error => {
          this.$message.error("注销失败 " + (error && error.toString()));
        }
      );
    },
    getChapterListByRule() {
      return Axios.post("/getChapterListByRule", this.importBookInfo).then(
        res => {
          if (res.data.isSuccess && res.data.data.book) {
            this.importBookInfo = res.data.data.book;
            this.importBookChapters = res.data.data.chapters;
          }
        },
        error => {
          this.$message.error("注销失败 " + (error && error.toString()));
        }
      );
    },
    showUserManageDialog() {
      eventBus.$emit("showUserManageDialog");
    },
    showMPCode() {
      eventBus.$emit("showMPCodeDialog");
    },
    joinTGChannel() {
      window.open("https://t.me/facker_channel", "_target");
    },
    ensureLoadBookCover() {
      // 手动触发滚动事件，显示书籍封面图片
      this.$refs.bookList.dispatchEvent(new MouseEvent("scroll"));

      // 上面一步应该能搞定，下面再确认一下
      this.$refs.bookCoverList.forEach(v => {
        if (!v.show && isInContainer(v.$el, this.$refs.bookList)) {
          // console.log("not show ", v);
          v.show = true;
        }
      });
    }
  },
  computed: {
    ...mapGetters([
      "collapseMenu",
      "dialogWidth",
      "dialogSmallWidth",
      "dialogTop",
      "dialogContentHeight",
      "popupWidth"
    ]),
    config() {
      return this.$store.getters.config;
    },
    isNight() {
      return this.$store.getters.isNight;
    },
    themeColor() {
      if (this.$store.getters.isNight) {
        return {
          background: "#f7f7f7"
        };
      } else {
        return {
          background: "#222"
        };
      }
    },
    showModernShelf() {
      return (
        !this.$store.state.miniInterface && this.$store.getters.isNormalPage
      );
    },
    hasPcShelfTheme() {
      return (
        !this.$store.state.miniInterface &&
        !!this.$store.getters.currentThemeConfig.shelf
      );
    },
    pcShelfThemeStyle() {
      if (!this.hasPcShelfTheme) {
        return {};
      }
      const shelf = this.$store.getters.currentThemeConfig.shelf;
      return {
        "--shelf-page": shelf.page,
        "--shelf-nav": shelf.nav,
        "--shelf-panel": shelf.panel,
        "--shelf-card": shelf.card,
        "--shelf-card-hover": shelf.cardHover,
        "--shelf-text": shelf.text,
        "--shelf-muted": shelf.muted,
        "--shelf-line": shelf.line,
        "--shelf-accent": shelf.accent,
        "--shelf-shadow": shelf.shadow,
        "--shelf-card-shadow": shelf.cardShadow,
        "--shelf-input": shelf.input
      };
    },
    shelfMainCardStyle() {
      const themeConfig = this.$store.getters.currentThemeConfig || {};
      const background = themeConfig.shelf
        ? themeConfig.shelf.panel
        : themeConfig.content || themeConfig.popup;
      if (!background) {
        return {};
      }
      if (typeof background === "string") {
        return {
          background
        };
      }
      return Object.keys(background).reduce((style, key) => {
        if (background[key] !== null && background[key] !== undefined) {
          style[key] = background[key];
        }
        return style;
      }, {});
    },
    bookList() {
      return this.isSearchResult ? this.searchResult : this.showShelfBooks;
    },
    bookCoverList() {
      return this.bookList
        .filter(v => this.getBookCoverUrl(v))
        .map(v => this.getCover(this.getBookCoverUrl(v), true));
    },
    shelfBooks() {
      return this.$store.getters.shelfBooks;
    },
    showShelfBooks() {
      return this.getShowShelfBooks(this.showBookGroup);
    },
    searchResultMap() {
      return this.searchResult.reduce((c, v) => {
        c[this.getSearchResultIdentityKey(v)] = v;
        return c;
      }, {});
    },
    connectStatus() {
      return this.$store.state.connected
        ? `后端已连接`
        : this.connecting
        ? "正在连接后端服务器……"
        : "点击设置后端接口前缀";
    },
    connectType() {
      return this.$store.state.connected ? "success" : "danger";
    },
    readingRecent() {
      return this.$store.getters.readingBook &&
        this.$store.getters.readingBook.name
        ? this.$store.getters.readingBook
        : {
            name: "尚无阅读记录",
            bookUrl: "",
            index: 0
          };
    },
    modernCurrentBook() {
      if (!this.readingRecent.bookUrl) return this.readingRecent;
      return (
        this.shelfBooks.find(v => v.bookUrl === this.readingRecent.bookUrl) ||
        this.readingRecent
      );
    },
    modernUpdatedBooks() {
      return this.shelfBooks
        .filter(v => this.modernUnreadCount(v) > 0)
        .sort((a, b) => {
          const diff = this.modernUnreadCount(b) - this.modernUnreadCount(a);
          return diff || (b.lastCheckTime || 0) - (a.lastCheckTime || 0);
        })
        .slice(0, 3);
    },
    modernUnreadBookCount() {
      return this.shelfBooks.filter(v => this.modernUnreadCount(v) > 0).length;
    },
    modernUnreadChapterCount() {
      return this.shelfBooks.reduce(
        (count, book) => count + this.modernUnreadCount(book),
        0
      );
    },
    loginAuth() {
      return this.$store.state.loginAuth;
    },
    bookSourceList() {
      return this.$store.state.bookSourceList;
    },
    userNS: {
      get() {
        return this.$store.state.userNS;
      },
      set(val) {
        this.$store.commit("setUserNS", val);
        if (val) {
          this.$store.commit("setIsManagerMode", true);
        }
      }
    },
    userList: {
      get() {
        return this.$store.state.userList;
      },
      set(val) {
        this.$store.commit("setUserList", val);
      }
    },
    bookSourceShowList() {
      return this.isShowFailureBookSource
        ? this.bookSourceHealthList
        : this.bookSourceList;
    },
    bookSourceGroupList() {
      const groupsMap = {};
      this.bookSourceList.forEach(v => {
        if (v.bookSourceGroup) {
          groupsMap[v.bookSourceGroup] = (groupsMap[v.bookSourceGroup] | 0) + 1;
        }
      });
      const groups = [
        {
          name: "全部分组",
          value: "",
          count: this.bookSourceList.length
        }
      ];
      for (const i in groupsMap) {
        if (Object.hasOwnProperty.call(groupsMap, i)) {
          groups.push({
            name: i,
            value: i,
            count: groupsMap[i]
          });
        }
      }
      return groups;
    },
    bookSourceShowGroup() {
      if (!this.isShowFailureBookSource) {
        const groups = new Set();
        this.bookSourceShowList.forEach(v => {
          v.bookSourceGroup && groups.add(v.bookSourceGroup);
        });
        groups.add("未分组");
        return Array.from(groups);
      } else {
        return ["异常", "正常", "已禁用", "书架使用", "未使用"];
      }
    },
    bookSourceShowLength() {
      return this.bookSourceShowResult.length;
    },
    bookSourceShowResult() {
      if (!this.showSourceGroup) {
        return this.bookSourceShowList;
      }
      if (this.isShowFailureBookSource) {
        return this.bookSourceShowList.filter(v => {
          switch (this.showSourceGroup) {
            case "异常":
              return v.status === "invalid";
            case "正常":
              return v.status === "healthy";
            case "已禁用":
              return v.status === "disabled";
            case "书架使用":
              return (v.shelfBookCount || 0) > 0;
            case "未使用":
              return !(v.shelfBookCount || 0);
            default:
              return true;
          }
        });
      } else {
        return this.bookSourceShowList.filter(v =>
          this.showSourceGroup === "未分组"
            ? !v.bookSourceGroup
            : v.bookSourceGroup === this.showSourceGroup
        );
      }
    },
    bookSourceShowResultPageList() {
      const start =
        (this.bookSourcePagination.page - 1) * this.bookSourcePagination.size;
      if (start > this.bookSourceShowResult.length) {
        return [];
      }
      return this.bookSourceShowResult.slice(
        start,
        Math.min(
          start + this.bookSourcePagination.size,
          this.bookSourceShowResult.length
        )
      );
    },
    showBookGroup: {
      get() {
        if (!this.bookGroupDisplayList.length) return -1;
        return this.$store.state.shelfConfig.showBookGroup;
      },
      set(val) {
        this.$store.commit("setShelfConfig", {
          ...this.$store.state.shelfConfig,
          showBookGroup: val
        });
      }
    },
    showBookGroupString: {
      get() {
        return "" + this.showBookGroup;
      },
      set(val) {
        this.showBookGroup = +val;
      }
    },
    bookGroupSetList() {
      return this.$store.state.bookGroupList.filter(v => v.groupId > 0);
    },
    bookGroupDisplayList() {
      return this.$store.state.bookGroupList
        .filter(v => this.getShowShelfBooks(v.groupId).length && v.show)
        .sort((a, b) => a.order - b.order);
    },
    searchConfig: {
      get() {
        return this.$store.state.searchConfig;
      },
      set(val) {
        this.$store.commit("setSearchConfig", val);
      }
    },
    isShowTocRule() {
      try {
        return (
          this.importBookInfo &&
          this.importBookInfo.originName &&
          (this.importBookInfo.originName.toLowerCase().endsWith(".txt") ||
            this.importBookInfo.originName.toLowerCase().endsWith(".epub"))
        );
      } catch (e) {
        // console.log(e);
      }
      return false;
    },
    tocRuleList() {
      if (!this.importBookInfo || !this.importBookInfo.originName) {
        return [];
      }
      if (this.importBookInfo.originName.toLowerCase().endsWith(".txt")) {
        // txt
        return this.$store.state.txtTocRules;
      } else {
        // epub
        return [
          { name: "根据 Spin 获取章节，使用 Toc 补充章节名", rule: "spin+toc" },
          { name: "根据 Spin 获取章节，强制使用 Toc 章节名", rule: "spin<toc" },
          { name: "根据 Spin 获取章节", rule: "spin" },
          { name: "根据 Toc 获取章节，使用 Spin 补充章节名", rule: "toc+spin" },
          { name: "根据 Toc 获取章节，强制使用 Spin 章节名", rule: "toc<spin" },
          { name: "根据 Toc 获取章节", rule: "toc" }
        ];
      }
    }
  }
};
</script>

<style lang="stylus" scoped>
.index-wrapper {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: row;

  .navigation-wrapper {
    width: 260px;
    min-width: 260px;
    height: 100%;
    box-sizing: border-box;
    background-color: var(--ui-bg);
    border-right: 1px solid var(--ui-border);
    position: relative;
    padding-top: 0;
    padding-top: constant(safe-area-inset-top) !important;
    padding-top: env(safe-area-inset-top) !important;
    transition: background-color var(--ui-transition), border-color var(--ui-transition);

    .navigation-inner-wrapper {
      padding: 40px 28px 66px 28px;
      height: 100%;
      overflow-y: auto;
      box-sizing: border-box;
    }

    .navigation-title {
      font-size: 22px;
      font-weight: 700;
      font-family: var(--ui-font);
      color: var(--ui-text);
      letter-spacing: -0.3px;

      .version-text {
        float: right;
        font-size: 13px;
        line-height: 33px;
        font-weight: 400;
        color: var(--ui-text-muted);
        display: inline-block;
        cursor: pointer;
        transition: color var(--ui-transition);

        &:hover {
          color: var(--ui-accent);
        }
      }
    }

    .navigation-sub-title {
      font-size: 15px;
      font-weight: 500;
      font-family: var(--ui-font);
      margin-top: 14px;
      color: var(--ui-text-muted);
    }

    .search-wrapper {
      .search-input {
        border-radius: 50%;
        margin-top: 20px;

        >>> .el-input__inner {
          border-radius: 50px;
          border-color: var(--ui-border);
          background: var(--ui-surface);
          transition: border-color var(--ui-transition), box-shadow var(--ui-transition);
        }
      }
    }

    .recent-wrapper {
      margin-top: 32px;

      .recent-title {
        font-size: 12px;
        font-weight: 600;
        color: var(--ui-text-muted);
        font-family: var(--ui-font);
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .reading-recent {
        margin: 14px 0;

        .recent-book {
          cursor: pointer;
          max-width: 100%;
          overflow: hidden;
          text-overflow: ellipsis;
          padding: 6px 8px;
          border-radius: var(--ui-radius-sm);
          transition: background var(--ui-transition), color var(--ui-transition);

          &:hover {
            background: rgba(79,110,247,.06);
            color: var(--ui-accent);
          }
        }
      }
    }

    .setting-wrapper {
      margin-top: 32px;

      .setting-title {
        font-size: 12px;
        font-weight: 600;
        color: var(--ui-text-muted);
        font-family: var(--ui-font);
        text-transform: uppercase;
        letter-spacing: 0.5px;

        .right-text {
          float: right;
          display: inline-block;
          height: 20px;
          line-height: 20px;
          cursor: pointer;
          user-select: none;
          color: var(--ui-accent);
          transition: opacity var(--ui-transition);

          &:hover {
            opacity: 0.8;
          }
        }
      }

      .no-point {
        pointer-events: none;
      }

      .setting-connect {
        cursor: pointer;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        padding: 6px 8px;
        border-radius: var(--ui-radius-sm);
        transition: background var(--ui-transition);

        &:hover {
          background: rgba(79,110,247,.06);
        }
      }

      .setting-item {
        padding-top: 14px;
      }

      .setting-btn {
        margin-right: 10px;
        margin-bottom: 10px;
        cursor: pointer;
        border-radius: var(--ui-radius-sm);
        border-color: var(--ui-border);
        transition: all var(--ui-transition);

        &:hover {
          border-color: var(--ui-accent);
          color: var(--ui-accent);
        }
      }

      .setting-select {
        width: 100%;
      }
    }

    .search-setting {
      margin-top: 24px;
    }

    .bottom-icons {
      position: absolute;
      bottom: 24px;
      width: 196px;
      left: 28px;
      align-items: center;
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      pointer-events: none;

      .bottom-icon {
        height: 36px;
        pointer-events: all;
        img {
          width: 36px;
          height: 36px;
          border-radius: var(--ui-radius-sm);
          transition: transform var(--ui-transition);

          &:hover {
            transform: scale(1.08);
          }
        }
      }

      .theme-item {
        line-height: 34px;
        width: 36px;
        height: 36px;
        border-radius: 50%;
        display: inline-block;
        cursor: pointer;
        text-align: center;
        vertical-align: middle;
        pointer-events: all;
        transition: transform var(--ui-transition), box-shadow var(--ui-transition);

        &:hover {
          transform: scale(1.1);
          box-shadow: var(--ui-shadow-sm);
        }

        .el-icon-moon {
          color: #f7f7f7;
          line-height: 34px;
        }
        .el-icon-sunny {
          color: #121212;
          line-height: 34px;
        }
      }
    }

    .setting-wrapper:nth-last-child(1) {
      padding-bottom: 20px;
    }
  }

  .shelf-wrapper {
    padding: 40px 40px;
    height: 100%;
    max-height: 100%;
    width: 100%;
    background-color: var(--ui-surface);
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    transition: background-color var(--ui-transition);

    .shelf-main-card {
      flex: 1 1 auto;
      min-height: 0;
      display: flex;
      flex-direction: column;
      box-sizing: border-box;
      padding: 24px;
      border: 1px solid var(--ui-border);
      border-radius: var(--ui-radius);
      background: var(--ui-surface);
      box-shadow: var(--ui-shadow-sm);
      overflow: hidden;
      transition: background var(--ui-transition), box-shadow var(--ui-transition), border-color var(--ui-transition);
    }

    .shelf-title {
      font-size: 20px;
      font-weight: 700;
      font-family: var(--ui-font);
      color: var(--ui-text);
      margin-bottom: 8px;
      min-width: 320px;
      box-sizing: border-box;

      .el-icon-menu {
        cursor: pointer;
        transition: color var(--ui-transition);

        &:hover {
          color: var(--ui-accent);
        }
      }

      .title-btn {
        font-size: 14px;
        line-height: 28px;
        float: right;
        cursor: pointer;
        user-select: none;
        margin-left: 10px;
        color: var(--ui-text-secondary);
        transition: color var(--ui-transition);

        &:hover {
          color: var(--ui-accent);
        }

        >>>.el-icon-loading {
          font-size: 16px;
        }
      }
    }

    >>>.el-icon-loading {
      font-size: 36px;
      color: var(--ui-text-muted);
    }

    >>>.el-loading-text {
      font-weight: 500;
      color: var(--ui-text-muted);
    }

    .book-group-wrapper {
      padding: 5px 0;
      margin-bottom: 10px;

      .book-group-tabs {
        width: 100%;
      }

      .book-group-btn {
        margin-right: 10px;
        cursor: pointer;
        border-radius: var(--ui-radius-sm);
        transition: all var(--ui-transition);
      }

      .book-group-btn.selected {
        color: #fff;
        background: var(--ui-accent);
        border-color: var(--ui-accent);
      }
    }

    .books-wrapper {
      flex: 1 1 auto;
      min-height: 0;
      overflow-x: hidden;
      overflow-y: auto;

      .wrapper {
        display: grid ;
        grid-template-columns: repeat(auto-fill, 380px);
        justify-content: space-around;
        grid-gap: 14px;

        .book {
          user-select: none;
          display: flex;
          cursor: pointer;
          margin-bottom: 14px;
          padding: 20px 22px;
          width: 360px;
          flex-direction: row;
          justify-content: space-around;
          border-radius: var(--ui-radius);
          transition: background var(--ui-transition), box-shadow var(--ui-transition), transform var(--ui-transition);

          &:hover {
            background: rgba(79,110,247,.04);
            box-shadow: var(--ui-shadow-sm);
            transform: translateY(-1px);
          }

          .cover-img {
            width: 84px;
            height: 112px;

            .cover {
              width: 84px;
              height: 112px;
              border-radius: 6px;
              box-shadow: 0 4px 12px rgba(0,0,0,.12);
            }
          }

          .info {
            position: relative;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            align-items: left;
            height: 112px;
            margin-left: 18px;
            flex: 1;

            .book-operation {
              position: absolute;
              right: 5px;
              top: 0px;
              font-size: 22px;
              color: var(--ui-text-muted);
              transition: color var(--ui-transition);

              &:hover {
                color: var(--ui-accent);
              }

              i {
                margin-left: 10px;
              }
            }

            .name {
              width: fit-content;
              font-size: 16px;
              font-weight: 700;
              color: var(--ui-text);
              margin-right: 38px;
              max-height: 45px;
              word-wrap: break-word;
              overflow: hidden;
              text-overflow: ellipsis;
              display: -webkit-box;
              -webkit-box-orient: vertical;
              -webkit-line-clamp: 2;
              letter-spacing: -0.2px;
            }

            .name.edit {
              margin-right: 62px;
            }

            .sub {
              display: flex;
              flex-direction: row;
              font-size: 12px;
              font-weight: 500;
              color: var(--ui-text-muted);

              .dot {
                margin: 0 7px;
              }
            }

            .intro, .dur-chapter, .last-chapter {
              color: var(--ui-text-secondary);
              font-size: 13px;
              margin-top: 3px;
              font-weight: 400;
              word-wrap: break-word;
              overflow: hidden;
              text-overflow: ellipsis;
              display: -webkit-box;
              -webkit-box-orient: vertical;
              -webkit-line-clamp: 1;
              text-align: left;
            }
          }
        }
      }

      .wrapper:last-child {
        margin-right: auto;
      }
    }

    .books-wrapper::-webkit-scrollbar {
      width: 0 !important;
    }
  }

  &.pc-shelf-theme {
    background: var(--shelf-page);

    .navigation-wrapper {
      background: var(--shelf-nav) !important;
      border-right: 1px solid var(--shelf-line);
      box-shadow: var(--shelf-shadow);
      backdrop-filter: blur(16px);

      .navigation-title {
        color: var(--shelf-text);
      }

      .navigation-sub-title,
      .setting-title,
      .recent-title {
        color: var(--shelf-muted);
      }

      .search-input {
        >>>.el-input__inner {
          background: var(--shelf-input);
          border-color: var(--shelf-line) !important;
          color: var(--shelf-text);
        }

        >>>.el-input__prefix {
          color: var(--shelf-muted);
        }
      }

      .setting-btn,
      .setting-connect,
      .recent-book {
        border-color: var(--shelf-line);
      }
    }

    .shelf-wrapper {
      background: var(--shelf-page);
      color: var(--shelf-text);
    }

    .shelf-main-card {
      border-color: var(--shelf-line);
      box-shadow: var(--shelf-shadow);
      color: var(--shelf-text);
    }

    .shelf-title {
      color: var(--shelf-text);

      .title-btn {
        color: var(--shelf-accent);
      }
    }

    .book-group-wrapper {
      >>>.el-tabs__nav-wrap::after {
        background-color: var(--shelf-line);
      }

      >>>.el-tabs__item {
        color: var(--shelf-muted);
      }

      >>>.el-tabs__item.is-active,
      >>>.el-tabs__item:hover {
        color: var(--shelf-accent);
      }

      >>>.el-tabs__active-bar {
        background-color: var(--shelf-accent);
      }

      .book-group-btn {
        border-color: var(--shelf-line);
        background: var(--shelf-card);
        color: var(--shelf-muted);
      }

      .book-group-btn.selected {
        background: var(--shelf-accent);
        border-color: var(--shelf-accent);
      }
    }

    .books-wrapper {
      .wrapper {
        grid-gap: 16px;

        .book {
          width: 360px;
          border: 1px solid var(--shelf-line);
          border-radius: 8px;
          background: var(--shelf-card);
          box-shadow: var(--shelf-card-shadow);
          transition: transform 160ms ease, background 160ms ease, box-shadow 160ms ease;

          &:hover {
            transform: translateY(-2px);
            background: var(--shelf-card-hover);
          }

          .cover-img {
            .cover {
              border-radius: 4px;
              box-shadow: 0 10px 22px rgba(0, 0, 0, 0.16);
            }
          }

          .info {
            .book-operation {
              color: var(--shelf-muted);
            }

            .name {
              color: var(--shelf-text);
            }

            .sub {
              color: var(--shelf-muted);
            }

            .intro,
            .dur-chapter,
            .last-chapter {
              color: var(--shelf-muted);
            }
          }
        }
      }
    }
  }

  &.modern-shelf-layout {
    --modern-bg: #f5f6f8;
    --modern-surface: #ffffff;
    --modern-soft: #eef0f4;
    --modern-raised: rgba(255, 255, 255, 0.92);
    --modern-text: #1a1d24;
    --modern-muted: #6b7280;
    --modern-weak: #9ca3af;
    --modern-line: rgba(0, 0, 0, 0.06);
    --modern-accent: #4f6ef7;
    --modern-accent-2: #10b981;
    --modern-danger: #ef4444;
    --modern-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    --modern-card-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    background: var(--modern-bg);

    &.night {
      --modern-bg: #111318;
      --modern-surface: #1a1d24;
      --modern-soft: #22252e;
      --modern-raised: #1e2128;
      --modern-text: #e5e7eb;
      --modern-muted: #9ca3af;
      --modern-weak: #6b7280;
      --modern-line: rgba(255, 255, 255, 0.07);
      --modern-accent: #6b8aff;
      --modern-accent-2: #34d399;
      --modern-danger: #f87171;
      --modern-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
      --modern-card-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);

      .navigation-wrapper {
        background: var(--modern-surface) !important;
        border-right-color: var(--modern-line) !important;
      }

      .shelf-wrapper.modern-shelf {
        background: var(--modern-bg);
      }

      .modern-continue-card {
        background: var(--modern-surface);
      }

      .modern-continue-cover .cover {
        box-shadow: 0 12px 28px rgba(0, 0, 0, 0.4);
      }

      .books-wrapper .wrapper .book {
        &:hover .cover-img .cover {
          box-shadow: 0 16px 36px rgba(0, 0, 0, 0.5);
        }

        .cover-img .cover {
          box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
        }
      }

      .el-input__inner {
        background: var(--modern-soft) !important;
        border-color: transparent !important;
        color: var(--modern-text) !important;

        &:focus {
          border-color: var(--modern-accent) !important;
          box-shadow: 0 0 0 3px rgba(107, 138, 255, 0.15) !important;
        }
      }

      .modern-icon-btn,
      .modern-text-btn:not(.primary) {
        background: var(--modern-soft);
        color: var(--modern-text);

        &:hover {
          background: #2d313b;
        }
      }

      .modern-text-btn.primary {
        background: var(--modern-accent);

        &:hover {
          background: #5a7aff;
        }
      }
    }

    .navigation-wrapper {
      width: 240px;
      min-width: 240px;
      background: var(--modern-surface) !important;
      border-right: 1px solid var(--modern-line);
      box-shadow: none;

      .navigation-inner-wrapper {
        padding: 20px 14px 76px;
      }

      .navigation-title {
        padding: 8px 8px 16px;
        border-bottom: 1px solid var(--modern-line);
        color: var(--modern-text);
        font-size: 20px;
        line-height: 42px;

        &::before {
          content: "阅";
          width: 42px;
          height: 42px;
          margin-right: 11px;
          display: inline-grid;
          place-items: center;
          border-radius: 8px;
          background: var(--modern-text);
          color: #fff;
          font-size: 22px;
          font-weight: 900;
          vertical-align: top;
        }

        .version-text {
          line-height: 42px;
          color: var(--modern-weak);
        }
      }

      .navigation-sub-title {
        margin: -8px 8px 18px 61px;
        color: var(--modern-muted);
        font-size: 13px;
      }

      .search-wrapper {
        display: none;
      }

      .setting-wrapper {
        margin-top: 18px;
        padding: 0 6px;

        .setting-title {
          color: var(--modern-weak);
          font-size: 12px;
          font-weight: 800;
        }

        .setting-item {
          padding-top: 10px;
        }

        .setting-btn,
        .setting-connect {
          max-width: 100%;
          margin-right: 6px;
          margin-bottom: 8px;
          border-color: var(--modern-line);
          border-radius: 8px;
          background: var(--modern-surface);
        }
      }
    }

    .modern-nav {
      min-height: 100%;
      display: grid;
      grid-template-rows: auto auto 1fr auto;
      gap: 18px;
    }

    .modern-brand {
      padding: 6px 8px 14px;
      display: grid;
      grid-template-columns: 36px 1fr;
      gap: 10px;
      align-items: center;
      border-bottom: 1px solid var(--modern-line);

      h1 {
        margin: 0;
        color: var(--modern-text);
        font-size: 16px;
        font-weight: 800;
        line-height: 1.2;
      }

      p {
        margin: 2px 0 0;
        color: var(--modern-weak);
        font-size: 11px;
        line-height: 1.35;
      }
    }

    .modern-brand-mark {
      width: 36px;
      height: 36px;
      display: grid;
      place-items: center;
      border-radius: 10px;
      background: linear-gradient(135deg, var(--modern-accent), var(--modern-accent-2));
      color: #fff;
      font-size: 18px;
      font-weight: 900;
    }

    .modern-nav-section {
      display: grid;
      gap: 8px;
    }

    .modern-nav-title {
      padding: 0 10px;
      color: var(--modern-weak);
      font-size: 12px;
      font-weight: 800;
    }

    .modern-nav-item {
      width: 100%;
      height: 38px;
      padding: 0 10px;
      display: grid;
      grid-template-columns: 22px 1fr auto;
      gap: 8px;
      align-items: center;
      border: 0;
      border-radius: 8px;
      background: transparent;
      color: var(--modern-muted);
      cursor: pointer;
      font-size: 13px;
      font-weight: 600;
      text-align: left;
      transition: all 180ms ease;

      &:hover {
        background: var(--modern-soft);
        color: var(--modern-text);
      }

      &.active {
        background: rgba(79, 110, 247, 0.10);
        color: var(--modern-accent);
        font-weight: 700;
      }

      &.disabled {
        opacity: 0.4;
        pointer-events: none;
      }

      em {
        font-style: normal;
      }

      strong {
        color: var(--modern-weak);
        font-size: 11px;
        font-weight: 700;
      }
    }

    .modern-action-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;

      button {
        min-height: 34px;
        padding: 0 8px;
        border: 1px solid var(--modern-line);
        border-radius: 8px;
        background: var(--modern-surface);
        color: var(--modern-muted);
        cursor: pointer;
        font-size: 12px;
        font-weight: 800;
      }
    }

    .modern-server-card {
      align-self: end;
      padding: 12px;
      display: grid;
      grid-template-columns: 8px 1fr;
      gap: 6px;
      align-items: center;
      border-radius: 10px;
      background: var(--modern-soft);

      strong {
        grid-column: 1 / -1;
        color: var(--modern-text);
        font-size: 12px;
        font-weight: 600;
        line-height: 1.35;
      }

      span {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--modern-danger);

        &.online {
          background: var(--modern-accent-2);
        }
      }

      button {
        padding: 0;
        border: 0;
        background: transparent;
        color: var(--modern-accent);
        cursor: pointer;
        font-size: 11px;
        font-weight: 700;
        text-align: left;
      }
    }

    .shelf-wrapper.modern-shelf {
      padding: 28px 32px;
      gap: 22px;
      height: 100%;
      max-height: 100%;
      overflow-y: auto;
      overflow-x: hidden;
      background: var(--modern-bg);
      color: var(--modern-text);
    }

    .modern-topbar {
      min-height: 52px;
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
    }

    .modern-search-controls {
      display: flex;
      gap: 10px;
      flex: 1;
      min-width: 280px;

      .search-history-wrapper {
        flex: 1;
        min-width: 0;
      }
    }

    .modern-source-group-select {
      min-width: 140px;
      max-width: 200px;

      >>>.el-input__inner {
        height: 40px;
        line-height: 40px;
        border-color: transparent;
        border-radius: 10px;
        background: var(--modern-soft);
        color: var(--modern-text);
        font-size: 13px;
        transition: all 200ms ease;

        &:focus {
          border-color: var(--modern-accent);
          background: var(--modern-surface);
          box-shadow: 0 0 0 3px rgba(79, 110, 247, 0.12);
        }
      }
    }

    .modern-search-input {
      flex: 1;
      min-width: 200px;

      >>>.el-input__inner {
        height: 40px;
        line-height: 40px;
        border-color: transparent;
        border-radius: 10px;
        background: var(--modern-soft);
        color: var(--modern-text);
        font-size: 13px;
        transition: all 200ms ease;

        &:focus {
          border-color: var(--modern-accent);
          background: var(--modern-surface);
          box-shadow: 0 0 0 3px rgba(79, 110, 247, 0.12);
        }
      }

      >>>.el-input__prefix {
        color: var(--modern-weak);
      }
    }

    .modern-top-actions {
      display: flex;
      gap: 8px;
      align-items: center;
    }

    .modern-icon-btn,
    .modern-text-btn {
      border: 1px solid transparent;
      border-radius: 10px;
      background: var(--modern-soft);
      color: var(--modern-text);
      cursor: pointer;
      transition: all 200ms ease;

      &:hover {
        background: var(--modern-line);
      }
    }

    .modern-icon-btn {
      width: 40px;
      height: 40px;
      font-size: 17px;
    }

    .modern-text-btn {
      height: 40px;
      padding: 0 16px;
      font-size: 13px;
      font-weight: 700;

      &.primary {
        border-color: transparent;
        background: var(--modern-accent);
        color: #fff;

        &:hover {
          background: var(--modern-accent-2);
        }
      }

      &.disabled {
        opacity: 0.45;
        pointer-events: none;
      }

      &[disabled] {
        cursor: default;
      }
    }

    .modern-overview {
      display: grid;
      grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr);
      gap: 20px;
    }

    .shelf-main-card {
      flex: 0 0 auto;
      gap: 0;
      min-height: 2480px;
      padding: 22px;
      border: 1px solid var(--modern-line);
      border-radius: 14px;
      box-shadow: var(--modern-shadow);
    }

    .modern-continue-card,
    .modern-update-card,
    .books-wrapper {
      border: 1px solid var(--modern-line);
      border-radius: 14px;
      background: var(--modern-surface);
      overflow: hidden;
    }

    .modern-continue-card {
      min-height: 240px;
      padding: 24px;
      display: grid;
      grid-template-columns: 148px 1fr;
      gap: 24px;
      cursor: pointer;
      background: var(--modern-surface);
      transition: box-shadow 300ms ease;

      &:hover {
        box-shadow: 0 8px 32px rgba(38, 48, 66, 0.10);
      }

      &.disabled {
        cursor: default;
      }
    }

    .modern-continue-cover {
      width: 148px;
      height: 200px;

      .cover {
        width: 148px;
        height: 200px;
        border-radius: 10px;
        box-shadow: 0 12px 28px rgba(38, 48, 66, 0.18);
        transition: transform 300ms ease;

        &.empty-cover {
          background:
            linear-gradient(145deg, rgba(79, 110, 247, 0.15), rgba(19, 160, 143, 0.15)),
            var(--modern-soft);
        }
      }
    }

    .modern-continue-info {
      min-width: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      gap: 10px;

      h2 {
        margin: 4px 0 0;
        font-size: clamp(22px, 3vw, 32px);
        line-height: 1.15;
        letter-spacing: -0.01em;
      }

      p {
        max-width: 600px;
        margin-top: 4px;
        color: var(--modern-muted);
        font-size: 13px;
        line-height: 1.5;
      }
    }

    .modern-hero-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
      margin-top: 4px;

      .modern-text-btn {
        height: 34px;
        padding: 0 14px;
        border-radius: 8px;
        font-size: 13px;
      }
    }

    .modern-eyebrow {
      color: var(--modern-accent);
      font-size: 11px;
      font-weight: 800;
      letter-spacing: 0.06em;
      text-transform: uppercase;
    }

    .modern-meta-row {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;

      span {
        min-height: 24px;
        padding: 4px 10px;
        border-radius: 6px;
        background: var(--modern-soft);
        color: var(--modern-muted);
        font-size: 11px;
        font-weight: 700;
      }
    }

    .modern-progress-row,
    .modern-book-progress {
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 10px;
      align-items: center;
      color: var(--modern-muted);
      font-size: 12px;
      font-weight: 700;
    }

    .modern-progress-track,
    .modern-book-progress span {
      height: 6px;
      overflow: hidden;
      border-radius: 999px;
      background: rgba(101, 113, 129, 0.12);

      i {
        display: block;
        height: 100%;
        border-radius: inherit;
        background: linear-gradient(90deg, var(--modern-accent), var(--modern-accent-2));
      }
    }

    .modern-book-progress {
      margin-top: auto;
      padding-top: 6px;

      span {
        height: 4px;
      }

      em {
        color: var(--modern-weak);
        font-size: 11px;
        font-style: normal;
      }
    }

    .modern-update-card {
      box-sizing: border-box;
      padding: 20px;
      display: grid;
      grid-template-rows: auto 1fr auto;
      gap: 14px;

      * {
        box-sizing: border-box;
      }
    }

    .modern-panel-head {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      align-items: flex-start;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 800;
        line-height: 1.3;
      }

      p {
        margin: 3px 0 0;
        color: var(--modern-muted);
        font-size: 12px;
        line-height: 1.45;
      }

      > span {
        padding: 3px 10px;
        border-radius: 6px;
        background: rgba(79, 110, 247, 0.08);
        color: var(--modern-accent);
        font-size: 13px;
        font-weight: 800;
        line-height: 22px;
        white-space: nowrap;
      }
    }

    .modern-update-list {
      display: grid;
      gap: 8px;
    }

    .modern-update-item {
      min-height: 54px;
      padding: 8px 10px;
      display: grid;
      grid-template-columns: 36px 1fr auto;
      gap: 10px;
      align-items: center;
      border: 1px solid transparent;
      border-radius: 10px;
      background: var(--modern-soft);
      cursor: pointer;
      transition: all 200ms ease;

      &:hover {
        border-color: var(--modern-line);
        background: var(--modern-surface);
        box-shadow: 0 2px 8px rgba(38, 48, 66, 0.06);
      }

      em {
        min-width: 26px;
        height: 22px;
        padding: 0 6px;
        display: grid;
        place-items: center;
        border-radius: 6px;
        background: rgba(223, 75, 96, 0.10);
        color: var(--modern-danger);
        font-size: 11px;
        font-style: normal;
        font-weight: 800;
      }
    }

    .modern-mini-cover {
      width: 36px;
      height: 48px;
      border-radius: 5px;
      overflow: hidden;
    }

    .modern-update-info {
      min-width: 0;

      strong,
      span {
        display: block;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      strong {
        font-size: 13px;
        font-weight: 700;
        line-height: 1.3;
      }

      span {
        margin-top: 3px;
        color: var(--modern-muted);
        font-size: 11px;
        line-height: 1.35;
      }
    }

    .modern-empty-tip {
      padding: 16px;
      border-radius: 10px;
      background: var(--modern-soft);
      color: var(--modern-muted);
      font-size: 13px;
      text-align: center;
    }

    .modern-stat-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;

      div {
        padding: 10px;
        border-radius: 8px;
        background: var(--modern-soft);
        text-align: center;
      }

      strong,
      span {
        display: block;
      }

      strong {
        font-size: 18px;
        font-weight: 800;
        line-height: 1.1;
        color: var(--modern-accent);
      }

      span {
        margin-top: 3px;
        color: var(--modern-muted);
        font-size: 11px;
        font-weight: 600;
      }
    }

    .shelf-title {
      min-height: 56px;
      margin: 0;
      padding: 14px 4px 0;
      border: 0;
      border-radius: 0;
      background: transparent;
      color: var(--modern-text);
      font-size: 20px;
      font-weight: 800;

      .title-btn {
        height: 30px;
        padding: 0 12px;
        border: 0;
        border-radius: 8px;
        background: var(--modern-soft);
        color: var(--modern-muted);
        line-height: 30px;
        font-size: 12px;
        font-weight: 700;
        transition: all 200ms ease;

        &:hover {
          background: var(--modern-line);
          color: var(--modern-text);
        }
      }
    }

    .book-group-wrapper {
      margin: 0;
      padding: 0 4px 8px;
      border: 0;
      background: transparent;
    }

    .books-wrapper {
      flex: 1 1 auto;
      min-height: 0;
      overflow-x: hidden;
      overflow-y: auto;
      border: 0;
      border-radius: 0;
      background: transparent;
      box-shadow: none;

      .wrapper {
        padding: 4px;
        grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
        justify-content: stretch;
        align-items: stretch;
        grid-gap: 18px;

        .book {
          box-sizing: border-box;
          width: 100%;
          min-height: 138px;
          margin: 0;
          padding: 12px;
          display: grid;
          grid-template-columns: 68px minmax(0, 1fr);
          gap: 12px;
          align-items: start;
          position: relative;
          border: 1px solid var(--modern-line);
          border-radius: 8px;
          background: var(--modern-raised);
          box-shadow: var(--modern-card-shadow);
          justify-content: flex-start;
          transition: transform 150ms ease, box-shadow 150ms ease, border-color 150ms ease, background 150ms ease;

          &:hover {
            transform: translateY(-2px);
            border-color: rgba(79, 110, 247, 0.28);
            background: var(--modern-surface);
            box-shadow: 0 18px 38px rgba(38, 48, 66, 0.14);

            .cover-img .cover {
              box-shadow: 0 12px 24px rgba(38, 48, 66, 0.2);
            }
          }

          .cover-img {
            width: 68px;
            height: 96px;
            padding-bottom: 0;
            position: static;
            border-radius: 6px;
            overflow: hidden;

            .cover {
              position: static;
              width: 68px;
              height: 96px;
              border-radius: 6px;
              box-shadow: 0 10px 20px rgba(38, 48, 66, 0.18);
              transition: box-shadow 150ms ease;
            }
          }

          .info {
            min-width: 0;
            min-height: 96px;
            height: auto;
            margin-left: 0;
            margin-top: 0;
            display: grid;
            align-content: space-between;
            gap: 8px;

            .book-operation {
              position: absolute;
              right: 12px;
              top: 12px;
              z-index: 2;
              font-size: 20px;
              color: var(--modern-muted);
              text-shadow: none;

              .unread-num-badge {
                position: absolute;
                right: -8px;
                top: -10px;
              }
            }

            .name {
              width: auto;
              padding-right: 28px;
              color: var(--modern-text);
              font-size: 15px;
              line-height: 1.3;
              font-weight: 900;
              max-height: 39px;
              display: -webkit-box;
              -webkit-line-clamp: 2;
              -webkit-box-orient: vertical;
              overflow: hidden;
            }

            .sub,
            .intro,
            .dur-chapter,
            .last-chapter {
              color: var(--modern-muted);
              font-size: 12px;
              line-height: 1.4;
              font-weight: 500;
            }

            .sub,
            .dur-chapter,
            .last-chapter {
              min-width: 0;
              white-space: nowrap;
              overflow: hidden;
              text-overflow: ellipsis;
            }
          }
        }
      }
    }
  }
}

.pc-shelf-theme.modern-shelf-layout {
  --modern-bg: var(--shelf-page);
  --modern-surface: var(--shelf-panel);
  --modern-soft: var(--shelf-input);
  --modern-raised: var(--shelf-card);
  --modern-text: var(--shelf-text);
  --modern-muted: var(--shelf-muted);
  --modern-weak: var(--shelf-muted);
  --modern-line: var(--shelf-line);
  --modern-accent: var(--shelf-accent);
  --modern-shadow: var(--shelf-shadow);
  --modern-card-shadow: var(--shelf-card-shadow);
}

.book-context-menu {
  position: fixed;
  z-index: 3201;
  box-sizing: border-box;
  width: 156px;
  padding: 6px;
  border: 1px solid var(--ui-border, rgba(20, 26, 34, 0.1));
  border-radius: 8px;
  background: var(--ui-surface, #fff);
  box-shadow: var(--ui-shadow-lg, 0 16px 42px rgba(20, 26, 34, 0.18));
}

.book-context-menu-item {
  display: flex;
  align-items: center;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: 0;
  border-radius: 6px;
  outline: none;
  background: transparent;
  color: var(--ui-text, #1f2530);
  cursor: pointer;
  font-size: 14px;
  line-height: 36px;
  text-align: left;
  transition: background var(--ui-transition), color var(--ui-transition);

  i {
    flex: 0 0 auto;
    margin-right: 8px;
    font-size: 16px;
  }

  span {
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  &:hover,
  &:focus {
    background: rgba(79,110,247,.08);
    color: var(--ui-accent, #4f6ef7);
  }

  &.danger {
    color: #f56c6c;

    &:hover,
    &:focus {
      background: rgba(245,108,108,.1);
      color: #f56c6c;
    }
  }
}

.unread-num-badge {
  >>>.el-badge__content {
    border: none;
  }
}

.night {
  >>>.navigation-wrapper {
    background-color: var(--ui-bg);
    border-right: 1px solid rgba(255,255,255,.06);
  }
  >>>.navigation-title {
    color: var(--ui-text);
  }
  >>>.navigation-sub-title,
  >>>.setting-title,
  >>>.recent-title {
    color: var(--ui-text-muted);
  }
  >>>.shelf-title {
    color: var(--ui-text);
  }
  >>>.shelf-wrapper {
    background-color: var(--ui-surface);
  }
  >>>.el-input__inner {
    background-color: #2a2b32;
    border: 1px solid rgba(255,255,255,.08) !important;
    color: #e2e4e8;
  }
  .book .info .name {
    color: #e2e4e8 !important;
  }
  .book .info .book-operation {
    color: var(--ui-text-muted) !important;
  }
  .book-context-menu {
    border-color: rgba(255,255,255,.08);
    background: #1e1e24;
    box-shadow: 0 16px 42px rgba(0,0,0,.36);
  }
  .book-context-menu-item {
    color: #e2e4e8;
  }
  .book .info .sub {
    color: var(--ui-text-muted) !important;
  }
  .book .info .intro, .book .info .dur-chapter, .book .info .last-chapter {
    color: var(--ui-text-secondary) !important;
  }
  .book:hover {
    background: rgba(255,255,255,.04) !important;
  }

  >>>.check-tip {
    color: var(--ui-text-secondary);
  }
}

.pc-shelf-theme.night {
  >>>.navigation-wrapper {
    background: var(--shelf-nav) !important;
    border-right-color: rgba(255,255,255,.06) !important;
  }

  >>>.navigation-title,
  >>>.shelf-title,
  .book .info .name {
    color: var(--shelf-text) !important;
  }

  >>>.navigation-sub-title,
  >>>.setting-title,
  >>>.recent-title,
  .book .info .book-operation,
  .book .info .sub,
  .book .info .intro,
  .book .info .dur-chapter,
  .book .info .last-chapter {
    color: var(--shelf-muted) !important;
  }

  >>>.shelf-wrapper {
    background:
      radial-gradient(circle at 80% 8%, rgba(255, 255, 255, 0.04), transparent 30%),
      var(--shelf-panel) !important;
  }

  >>>.el-input__inner {
    background: var(--shelf-input) !important;
    border-color: rgba(255,255,255,.08) !important;
    color: var(--shelf-text) !important;
  }
}

.source-container {
  // max-height: 400px;
  // overflow-y: auto;
  padding: 0 10px;

  &.table-container {
    padding: 0;
  }

  .check-form {
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    align-items: center;

    .check-form-label {
      min-width: 60px;
    }

    .el-input {
      width: auto;
      min-width: 100px;
      margin-right: 10px;
    }

    .el-input-number {
      min-width: 130px;
      margin-right: 10px;
    }

    .book-cover {
      width: 84px;
      height: 112px;

      .cover {
        width: 84px;
        height: 112px;
      }
    }

    .book-info {
      display: flex;
      flex-direction: column;
      margin-left: 30px;
      justify-content: space-between;
      min-height: 100px;

      .toc-refresh-btn {
        margin-left: 5px;
      }

      span {
        display: inline-block;
        min-width: 56px;
        text-align-last: justify;
      }
      .el-input {
        width: auto;
        min-width: 100px;
        margin-right: 10px;
      }
      .el-input-number {
        min-width: 130px;
        margin-right: 10px;
      }
    }
  }

  .chapter-title {
    font-size: 15px;
    padding: 5px 0;
    font-weight: 600;
    margin-top: 10px;
  }

  .chapter-list {
    overflow-y: auto;
    box-sizing: border-box;
    padding: 0 5px;

    p {
      margin-top: 0.4em;
      margin-bottom: 0.4em;
    }
  }

  .source-group-wrapper {
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    padding: 5px 0;

    .source-group-btn {
      margin-right: 10px;
      cursor: pointer;
    }

    .source-group-btn.selected {
      color: #fff;
      background: #409EFF;
      border-color: #409EFF;
    }
  }

  .el-pagination {
    margin-top: 8px;
    float: right;
    max-width: 100%;
    overflow-x: auto;
    box-sizing: border-box;
  }

  >>>.source-checkbox {
    display: block;
    padding: 8px 0;
    width: 100%;
  }

  pre {
    margin: 0;
  }

  .source-pagination::after {
    display: table;
    content: "";
    clear: both;
  }
}

.source-list-container {
  max-height: calc(var(--vh, 1vh) * 70 - 54px - 60px - 66px);
  overflow-y: auto;
  overflow-x: auto;
}

.subscription-container {
  .subscription-url-form-item {
    width: 52%;

    >>>.el-form-item__content {
      width: calc(100% - 78px);
    }
  }
}

.night {
  .source-container {
    .source-group-wrapper {
      .source-group-btn.selected {
        color: #fff;
        background: var(--ui-accent) !important;
        border-color: var(--ui-accent) !important;
      }
    }
  }
  .book-group-wrapper {
    .book-group-btn.selected {
      color: #fff;
      background: var(--ui-accent) !important;
      border-color: var(--ui-accent) !important;
    }
  }
}

.source-container::-webkit-scrollbar {
  width: 0 !important;
}
.navigation-inner-wrapper::-webkit-scrollbar {
  width: 0 !important;
}
>>> .el-table__body-wrapper::-webkit-scrollbar {
  width: 0 !important;
}
>>> .el-dialog__wrapper::-webkit-scrollbar {
  width: 0 !important;
}
@media screen and (max-width: 750px) {
  .index-wrapper {
    overflow-x: hidden;

    >>>.navigation-wrapper {
      .navigation-inner-wrapper {
        padding: 20px 36px 66px 36px;
      }
    }
    >>>.shelf-wrapper {
      padding: 0;
      padding-top: constant(safe-area-inset-top) !important;
      padding-top: env(safe-area-inset-top) !important;

      .shelf-main-card {
        min-height: 0;
        padding: 0;
        border: 0;
        border-radius: 0;
        box-shadow: none;
      }

      .shelf-title {
        padding: 20px 24px 0 24px;
      }

      .book-group-wrapper {
        margin-left: 24px;
        margin-right: 24px;
      }

      &.modern-shelf {
        padding: 16px;

        .shelf-main-card {
          min-height: 0;
          padding: 16px;
          border: 1px solid var(--modern-line);
          border-radius: 14px;
          box-shadow: var(--modern-shadow);
        }

        .modern-topbar {
          flex-direction: column;
          align-items: stretch;
        }

        .modern-search-controls {
          flex-direction: column;
          min-width: 0;
        }

        .modern-source-group-select {
          max-width: 100%;
        }

        .modern-top-actions {
          justify-content: flex-start;
          flex-wrap: wrap;
        }

        .modern-overview {
          grid-template-columns: 1fr;
        }

        .modern-continue-card {
          grid-template-columns: 100px 1fr;
          min-height: auto;
          padding: 16px;
          gap: 16px;
        }

        .modern-continue-cover {
          width: 100px;
          height: 140px;

          .cover {
            width: 100px;
            height: 140px;
          }
        }
      }

      .books-wrapper {
        .wrapper {
          display: flex;
          flex-direction: column;
          grid-gap: 12px;
          padding: 4px;

          .book {
            box-sizing: border-box;
            width: 100%;
            margin-bottom: 0;
            padding: 10px 20px;
          }
        }
      }
    }
  }
  .source-list-container  {
    max-height: calc(var(--vh, 1vh) * 100 - 54px - 40px - 66px);
  }
}
@media screen and (max-width: 480px) {
  .source-container.table-container {
    margin: -15px -5px;
  }
}

.search-history-wrapper {
  position: relative;
}

.search-history-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 2000;
  background: var(--ui-surface, #fff);
  border: 1px solid var(--ui-border, #dcdfe6);
  border-radius: 8px;
  margin-top: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 300px;
  overflow-y: auto;

  .search-history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    font-size: 12px;
    color: var(--ui-text-secondary, #909399);
    border-bottom: 1px solid var(--ui-border, #ebeef5);

    .search-history-clear {
      cursor: pointer;

      &:hover {
        color: var(--ui-text, #303133);
      }
    }
  }

  .search-history-item {
    padding: 8px 12px;
    font-size: 13px;
    cursor: pointer;
    color: var(--ui-text, #606266);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &:hover {
      background: var(--ui-hover, #f5f7fa);
    }
  }
}
</style>
<style>
.navigation-hidden {
  margin-left: -260px;
}
.navigation-in {
  margin-left: 0px;
  transition: margin-left 0.3s;
}
.navigation-out {
  margin-left: -260px;
  transition: margin-left 0.3s;
}
.popper-intro {
  padding: 15px;
}
.book-kind span {
  display: inline-block;
  margin-left: 5px;
  margin-right: 5px;
}
.night-theme .popper-intro {
  background: #1e1e24;
  color: #c5c8ce !important;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: var(--ui-radius);
  box-shadow: var(--ui-shadow-lg);
}
.night-theme .popper-intro.el-popper[x-placement^="bottom"] .popper__arrow,
.night-theme
  .popper-intro.el-popper[x-placement^="bottom"]
  .popper__arrow::after {
  border-bottom-color: #1e1e24 !important;
}
.night-theme .popper-intro.el-popper[x-placement^="top"] .popper__arrow,
.night-theme .popper-intro.el-popper[x-placement^="top"] .popper__arrow::after {
  border-top-color: #1e1e24 !important;
}
.night-theme .el-popover__title {
  color: #e2e4e8 !important;
}
.status-bar-light-bg {
  background-image: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.2) 0,
    transparent 36px
  ) !important;
}
.status-bar-light-bg-dialog .el-dialog.is-fullscreen {
  background-image: linear-gradient(
    to bottom,
    rgba(0, 0, 0, 0.2) 0,
    transparent 36px
  ) !important;
}
.modern-action-popover {
  padding: 0 !important;
  border: 1px solid var(--ui-border) !important;
  border-radius: var(--ui-radius) !important;
  box-shadow: var(--ui-shadow-lg) !important;
}
.modern-action-panel {
  max-height: min(720px, calc(100vh - 32px));
  overflow-y: auto;
  padding: 16px;
  background: var(--ui-surface);
  color: var(--ui-text);
}
.modern-action-panel-head {
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(20, 26, 34, 0.1);
}
.modern-action-panel-head strong,
.modern-action-panel-head span {
  display: block;
}
.modern-action-panel-head strong {
  font-size: 16px;
  line-height: 1.3;
}
.modern-action-panel-head span {
  margin-top: 4px;
  color: var(--ui-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}
.modern-action-section {
  margin-top: 14px;
}
.modern-action-title {
  margin-bottom: 8px;
  color: var(--ui-text-muted);
  font-size: 12px;
  font-weight: 800;
}
.modern-action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
.modern-action-grid button {
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-sm);
  background: var(--ui-bg);
  color: var(--ui-text);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
  transition: all var(--ui-transition);
}
.modern-action-grid button:hover {
  border-color: rgba(79, 110, 247, 0.26);
  background: rgba(79, 110, 247, 0.08);
  color: var(--ui-accent);
}
@media (hover: hover) {
  .book:hover {
    background: rgba(79, 110, 247, 0.04);
    transition: background var(--ui-transition);
  }
  .el-icon-close:hover {
    color: var(--ui-accent);
  }
  .el-icon-edit:hover {
    color: var(--ui-accent);
  }
}

.mini-interface .el-dialog__body {
  padding: 15px 20px;
}
.book-group-tabs .el-tabs__header {
  margin-bottom: 0px;
}
.modern-shelf-layout .book-group-tabs .el-tabs__header {
  margin-bottom: 0;
}
.modern-shelf-layout .book-group-tabs .el-tabs__nav-wrap::after {
  display: none;
}
.modern-shelf-layout .book-group-tabs .el-tabs__active-bar {
  height: 3px;
  border-radius: 2px;
  background: var(--modern-accent, #4f6ef7);
}
.modern-shelf-layout .book-group-tabs .el-tabs__item {
  height: 36px;
  line-height: 36px;
  font-size: 13px;
  font-weight: 600;
  color: var(--modern-muted, #6b7280);
  transition: color 200ms ease;
}
.modern-shelf-layout .book-group-tabs .el-tabs__item.is-active {
  color: var(--modern-accent, #4f6ef7);
  font-weight: 700;
}
.modern-shelf-layout .book-group-tabs .el-tabs__item:hover {
  color: var(--modern-text, #1a1d24);
}
</style>
