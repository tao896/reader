const SKIP_TEXT_TAGS = {
  script: true,
  style: true,
  noscript: true,
  textarea: true,
  input: true,
  select: true,
  option: true
};

const EMPTY_BLOCK_SELECTOR =
  "p,div,section,article,li,blockquote,h1,h2,h3,h4,h5,h6";

const MEDIA_SELECTOR = "img,svg,video,audio,canvas,iframe,object,embed,table";

export const getMatchedReplaceRules = (filterRules, readingBook) => {
  const book = readingBook || {};
  const bookName = book.name || "";
  const bookUrl = book.bookUrl || "";
  return (filterRules || []).filter(rule => {
    if (!rule || !rule.pattern) {
      return false;
    }
    if (typeof rule.isEnabled !== "undefined" && rule.isEnabled === false) {
      return false;
    }
    const scopeText =
      typeof rule.scope === "string"
        ? rule.scope.replace(/^\s+|\s+$/g, "")
        : "";
    const scope = scopeText ? scopeText.split(";") : [];
    const isGlobalScope = !scopeText || scope[0] === "*";
    const isBookMatched = isGlobalScope || scope[0] === bookName;
    const isBookUrlMatched =
      isGlobalScope || scope.length === 1 || !scope[1] || scope[1] === bookUrl;
    return isBookMatched && isBookUrlMatched;
  });
};

export const normalizeReplaceRules = (filterRules, readingBook) => {
  return getMatchedReplaceRules(filterRules, readingBook).map(rule => ({
    pattern: rule.pattern,
    replacement: rule.replacement || "",
    isRegex: rule.isRegex === true
  }));
};

export const applyPreparedReplaceRulesToText = (content, rules) => {
  if (!content || !rules || !rules.length) {
    return content;
  }
  let result = content;
  rules.forEach(rule => {
    if (!rule || !rule.pattern) {
      return;
    }
    try {
      if (rule.isRegex) {
        result = result.replace(
          new RegExp(rule.pattern, "ig"),
          rule.replacement
        );
      } else {
        result = result.split(rule.pattern).join(rule.replacement);
      }
    } catch (error) {
      //
    }
  });
  return result;
};

export const applyReplaceRulesToText = (content, filterRules, readingBook) => {
  return applyPreparedReplaceRulesToText(
    content,
    normalizeReplaceRules(filterRules, readingBook)
  );
};

const shouldSkipTextNode = node => {
  let parent = node && node.parentNode;
  while (parent && parent.nodeType === 1) {
    const tagName = (parent.nodeName || "").toLowerCase();
    if (SKIP_TEXT_TAGS[tagName]) {
      return true;
    }
    parent = parent.parentNode;
  }
  return false;
};

const trimText = value => {
  return (value || "").replace(/^\s+|\s+$/g, "");
};

const findClosestBlockElement = (node, root) => {
  let element = node && node.nodeType === 1 ? node : node && node.parentNode;
  while (element && element !== root.parentNode) {
    if (
      element.nodeType === 1 &&
      element.matches &&
      element.matches(EMPTY_BLOCK_SELECTOR)
    ) {
      return element;
    }
    if (element === root) {
      return null;
    }
    element = element.parentNode;
  }
  return null;
};

const restoreHiddenBlocks = root => {
  if (!root || !root.querySelectorAll) {
    return;
  }
  const hiddenBlocks = root.querySelectorAll("[data-reader-filter-hidden='1']");
  Array.prototype.forEach.call(hiddenBlocks, element => {
    element.style.display =
      element.getAttribute("data-reader-filter-display") || "";
    element.removeAttribute("data-reader-filter-hidden");
    element.removeAttribute("data-reader-filter-display");
  });
};

const hideEmptyBlocks = candidates => {
  if (!candidates || !candidates.length) {
    return;
  }
  const blocks = [];
  candidates.forEach(element => {
    if (element && blocks.indexOf(element) < 0) {
      blocks.push(element);
    }
  });
  blocks.forEach(element => {
    if (element.querySelector(MEDIA_SELECTOR)) {
      return;
    }
    if (trimText(element.textContent)) {
      return;
    }
    element.setAttribute("data-reader-filter-hidden", "1");
    element.setAttribute(
      "data-reader-filter-display",
      element.style.display || ""
    );
    element.style.display = "none";
  });
};

export const applyReplaceRulesToDocument = (
  doc,
  filterRules,
  readingBook,
  originalTextMap
) => {
  const root = doc && (doc.body || doc.documentElement);
  if (!root || !doc.createTreeWalker) {
    return {
      changed: false,
      count: 0
    };
  }
  const rules = normalizeReplaceRules(filterRules, readingBook);
  const textMap = originalTextMap || new WeakMap();
  let changed = false;
  let count = 0;
  const emptyBlockCandidates = [];
  const walker = doc.createTreeWalker(
    root,
    4,
    {
      acceptNode(node) {
        return shouldSkipTextNode(node) ? 2 : 1;
      }
    },
    false
  );
  let node = walker.nextNode();
  while (node) {
    if (!textMap.has(node)) {
      textMap.set(node, node.nodeValue || "");
    }
    const originalValue = textMap.get(node);
    const nextValue = applyPreparedReplaceRulesToText(originalValue, rules);
    if (node.nodeValue !== nextValue) {
      node.nodeValue = nextValue;
      changed = true;
      count += 1;
      if (trimText(originalValue) && !trimText(nextValue)) {
        emptyBlockCandidates.push(
          findClosestBlockElement(node.parentNode, root)
        );
      }
    }
    node = walker.nextNode();
  }
  restoreHiddenBlocks(root);
  hideEmptyBlocks(emptyBlockCandidates);
  return {
    changed,
    count
  };
};

export const buildEpubReplaceRulesScript = (
  filterRules,
  readingBook,
  token
) => {
  const rules = normalizeReplaceRules(filterRules, readingBook);
  return `
(function() {
  var rules = ${JSON.stringify(rules)};
  var token = ${JSON.stringify(token || "")};
  var skipTextTags = ${JSON.stringify(SKIP_TEXT_TAGS)};
  var emptyBlockSelector = ${JSON.stringify(EMPTY_BLOCK_SELECTOR)};
  var mediaSelector = ${JSON.stringify(MEDIA_SELECTOR)};
  var root = document.body || document.documentElement;
  window.reader_replace_rule_token = token;
  if (!root || !document.createTreeWalker) {
    if (typeof reader_notify === 'function') {
      reader_notify('replaceRuleApplied', { token: token, changed: false, count: 0 });
    }
    return;
  }
  function applyRules(text) {
    if (!text || !rules.length) {
      return text;
    }
    var result = text;
    rules.forEach(function(rule) {
      if (!rule || !rule.pattern) {
        return;
      }
      try {
        if (rule.isRegex) {
          result = result.replace(new RegExp(rule.pattern, 'ig'), rule.replacement);
        } else {
          result = result.split(rule.pattern).join(rule.replacement);
        }
      } catch (error) {}
    });
    return result;
  }
  function shouldSkipTextNode(node) {
    var parent = node && node.parentNode;
    while (parent && parent.nodeType === 1) {
      var tagName = (parent.nodeName || '').toLowerCase();
      if (skipTextTags[tagName]) {
        return true;
      }
      parent = parent.parentNode;
    }
    return false;
  }
  function trimText(value) {
    return (value || '').replace(/^\\s+|\\s+$/g, '');
  }
  function findClosestBlockElement(node) {
    var element = node && node.nodeType === 1 ? node : node && node.parentNode;
    while (element && element !== root.parentNode) {
      if (element.nodeType === 1 && element.matches && element.matches(emptyBlockSelector)) {
        return element;
      }
      if (element === root) {
        return null;
      }
      element = element.parentNode;
    }
    return null;
  }
  function restoreHiddenBlocks() {
    if (!root.querySelectorAll) {
      return;
    }
    var hiddenBlocks = root.querySelectorAll('[data-reader-filter-hidden="1"]');
    Array.prototype.forEach.call(hiddenBlocks, function(element) {
      element.style.display = element.getAttribute('data-reader-filter-display') || '';
      element.removeAttribute('data-reader-filter-hidden');
      element.removeAttribute('data-reader-filter-display');
    });
  }
  function hideEmptyBlocks(candidates) {
    if (!candidates || !candidates.length) {
      return;
    }
    var blocks = [];
    candidates.forEach(function(element) {
      if (element && blocks.indexOf(element) < 0) {
        blocks.push(element);
      }
    });
    blocks.forEach(function(element) {
      if (element.querySelector(mediaSelector)) {
        return;
      }
      if (trimText(element.textContent)) {
        return;
      }
      element.setAttribute('data-reader-filter-hidden', '1');
      element.setAttribute('data-reader-filter-display', element.style.display || '');
      element.style.display = 'none';
    });
  }
  var changed = false;
  var count = 0;
  var emptyBlockCandidates = [];
  var walker = document.createTreeWalker(root, 4, {
    acceptNode: function(node) {
      return shouldSkipTextNode(node) ? 2 : 1;
    }
  }, false);
  var node = walker.nextNode();
  while (node) {
    if (typeof node.__readerOriginalText !== 'string') {
      node.__readerOriginalText = node.nodeValue || '';
    }
    var originalValue = node.__readerOriginalText;
    var nextValue = applyRules(originalValue);
    if (node.nodeValue !== nextValue) {
      node.nodeValue = nextValue;
      changed = true;
      count += 1;
      if (trimText(originalValue) && !trimText(nextValue)) {
        emptyBlockCandidates.push(findClosestBlockElement(node.parentNode));
      }
    }
    node = walker.nextNode();
  }
  restoreHiddenBlocks();
  hideEmptyBlocks(emptyBlockCandidates);
  if (typeof reader_notify === 'function') {
    reader_notify('replaceRuleApplied', { token: token, changed: changed, count: count });
    reader_notify('setHeight', document.documentElement.scrollHeight || document.body.scrollHeight);
  }
})();
`;
};
