package com.htmake.reader.config

import java.io.File

object BookConfig {
    val javascriptVersion = "reader-inject-javascript-1.1.3"
    val epubInjectJavascript = """
    //<![CDATA[
    // ${javascriptVersion}
    if (!window.reader_inited) {
        window.nativeCallback = window.nativeCallback || {};

        function reader_notify(event, data, id) {
            if (window.self !== window.top) {
                window.top.postMessage(JSON.stringify({
                    id: id,
                    event: event,
                    data: data
                }), '*');
            }
        }

        var reader_style_dom = document.createElement('style');
        var head = document.head || document.getElementsByTagName('head')[0];
        head.appendChild(reader_style_dom);

        function reader_setStyle(style) {
            reader_style_dom.innerText = style;
            reader_notifyHeight();
            setTimeout(reader_notifyHeight, 100);
        }

        function reader_notifyHeight() {
            reader_notify("setHeight", document.documentElement.scrollHeight || document.body.scrollHeight)
        }

        function reader_rectToObject(rect) {
            if (!rect) {
                return null;
            }
            var left = typeof rect.left === 'number' ? rect.left : 0;
            var top = typeof rect.top === 'number' ? rect.top : 0;
            var right = typeof rect.right === 'number' ? rect.right : left;
            var bottom = typeof rect.bottom === 'number' ? rect.bottom : top;
            var width = typeof rect.width === 'number' ? rect.width : right - left;
            var height = typeof rect.height === 'number' ? rect.height : bottom - top;
            return {
                top: top,
                right: right,
                bottom: bottom,
                left: left,
                width: width,
                height: height
            };
        }

        function reader_getSelectionText() {
            if (window.getSelection) {
                return window.getSelection().toString();
            }
            if (document.selection && document.selection.type != 'Control') {
                return document.selection.createRange().text;
            }
            return '';
        }

        function reader_getSelectionRect() {
            try {
                if (window.getSelection) {
                    var selection = window.getSelection();
                    if (!selection || !selection.rangeCount) {
                        return null;
                    }
                    var range = selection.getRangeAt(0);
                    var rects = Array.prototype.slice.call(range.getClientRects()).filter(function(rect) {
                        return rect.width || rect.height;
                    });
                    if (!rects.length) {
                        return reader_rectToObject(range.getBoundingClientRect());
                    }
                    var rect = rects.reduce(function(result, item) {
                        var left = Math.min(result.left, item.left);
                        var top = Math.min(result.top, item.top);
                        var right = Math.max(result.right, item.right);
                        var bottom = Math.max(result.bottom, item.bottom);
                        return {
                            top: top,
                            right: right,
                            bottom: bottom,
                            left: left,
                            width: right - left,
                            height: bottom - top
                        };
                    }, reader_rectToObject(rects[0]));
                    return reader_rectToObject(rect);
                }
                if (document.selection && document.selection.type != 'Control') {
                    return reader_rectToObject(document.selection.createRange().getBoundingClientRect());
                }
            } catch (error) {
                return null;
            }
            return null;
        }

        function reader_notifySelection() {
            var text = reader_getSelectionText() || '';
            var pureText = text.replace(/^\s+/, '').replace(/\s+$/, '');
            if (!pureText) {
                return false;
            }
            reader_notify('selection', {
                text: pureText,
                rect: reader_getSelectionRect()
            });
            return true;
        }

        function reader_deferNotifySelection(delay) {
            setTimeout(reader_notifySelection, delay || 0);
        }

        function reader_listenFromParent(event) {
            if (window.self !== window.top && event.source !== window.parent && event.source !== window.top) {
                return;
            }

            let data;
            try {
                data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
            } catch (error) {
                // console.error(error);
                return;
            }

            if (!data || typeof data !== 'object') {
                return;
            }
            if (data.event === 'setStyle') {
                reader_setStyle(data.style);
            } else if (data.event === 'execute' && typeof data.script === 'string') {
                eval(data.script);
            } else if (data.id) {
                if (window.nativeCallback && typeof window.nativeCallback[data.id] === 'function') {
                    window.nativeCallback[data.id](data);
                    delete window.nativeCallback[data.id]
                }
            }
        }


        function reader_getLinkElement(element) {
            if (!element || !element.nodeName) {
                return false;
            }
            if (element.nodeName.toLowerCase() === "a") {
                return element;
            }
            return reader_getLinkElement(element.parentNode)
        }

        function reader_getImageElement(element) {
            if (!element || !element.nodeName) {
                return false;
            }
            if (element.nodeName.toLowerCase() === "img") {
                return element;
            }
        }

        window.document.addEventListener('message', reader_listenFromParent);
        window.addEventListener('message', reader_listenFromParent);
        window.addEventListener('load', function() {
            reader_notifyHeight();
            reader_notify("load", window.location.href);
        });
        window.addEventListener('resize', reader_notifyHeight);
        document.addEventListener('DOMNodeInserted', reader_notifyHeight, false);
        document.addEventListener('selectionchange', function() {
            reader_deferNotifySelection(180);
        }, false);
        document.addEventListener('mouseup', function() {
            reader_deferNotifySelection(0);
        }, false);
        document.addEventListener('touchend', function() {
            reader_deferNotifySelection(120);
        }, false);
        window.addEventListener('keyup', function() {
            reader_deferNotifySelection(0);
        });
        document.addEventListener('click', function(event) {
            if (reader_notifySelection()) {
                event.preventDefault();
                event.stopPropagation();
                return;
            }
            var linkElement = reader_getLinkElement(event.target)
            var imageElement = reader_getImageElement(event.target)
            if (linkElement) {
                // 点击链接跳转
                if (linkElement.pathname === window.location.pathname) {
                    // 页内跳转
                    var hashElement = document.querySelector(linkElement.hash)
                    if (hashElement) {
                        reader_notify("clickHash", hashElement.getBoundingClientRect())
                    }
                } else {
                    // 跳转其他页面
                    reader_notify("clickA", event.target.href);
                }
            } else if (imageElement) {
                var imgList = document.querySelectorAll("img");
                if (imgList.length) {
                    var imgUrlList = [];
                    var index = 0;
                    for (let i = 0; i < imgList.length; i++) {
                        imgUrlList.push(imgList[i].src);
                        if (imgList[i] === imageElement) {
                            index = i;
                        }
                    }
                    reader_notify("previewImageList", {
                        imageList: imgUrlList,
                        imageIndex: index
                    });
                }
            } else {
                reader_notify("click", {
                    target: event.target.nodeName,
                    clientX: event.clientX,
                    clientY: event.clientY
                });
            }
        });
        window.addEventListener("keydown", function(event) {
            event.preventDefault();
            event.stopPropagation();
            reader_notify("keydown", {
                key: event.key,
                keyCode: event.keyCode
            });
        });
        reader_notify("inited");

        window.reader_inited = true;
    }
    //]]>
    """

    fun injectJavascriptToEpubChapter(filePath: String) {
        val file = File(filePath);
        if (file.exists()) {
            var content = file.readText()
            if (content.indexOf(javascriptVersion) < 0) {
                content = content.replace("<head>", """<head><script type="text/javascript">${epubInjectJavascript}</script>""")
                file.writeText(content)
            }
        }
    }
}
