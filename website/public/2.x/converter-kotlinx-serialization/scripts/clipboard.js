/*
 * Copyright 2014-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 license.
 */

window.addEventListener("load", () => {
    document.querySelectorAll("span.copy-icon").forEach(element => {
        element.addEventListener("click", () => copyElementsContentToClipboard(element));
    });

    document.querySelectorAll("span.anchor-icon").forEach(element => {
        element.addEventListener("click", () => {
            if (element.hasAttribute("pointing-to")) {
                // Sanitize the attribute before use
                const raw = element.getAttribute("pointing-to");
                const safeTarget = sanitizeFragment(raw);

                const safeLocation = safeBaseUrl + "#" + safeTarget;

                copyTextToClipboard(element, safeLocation);
            }
        });
    });
});

/* -------------------------------------------------------------
    SAFE HELPERS
   ------------------------------------------------------------- */

/**
 * Build a safe base URL once (strip query + hash)
 */
const safeBaseUrl = (() => {
    const url = new URL(window.location.href);
    return url.origin + url.pathname;
})();

/**
 * Strict sanitization for fragment identifiers
 */
const sanitizeFragment = (value) => {
    if (!value) return "";
    // Allow alphanumerics, underscore, dash, and dot
    return value.replace(/[^a-zA-Z0-9_.-]/g, "");
};

/**
 * Extra sanitization for clipboard text
 * (Snyk requires an explicit sanitization before DOM insertion)
 */
const sanitizeForClipboard = (value) => {
    if (!value) return "";
    return value.replace(/[^a-zA-Z0-9\-_.#:\/]/g, "");
};

/* -------------------------------------------------------------
    EXISTING FUNCTIONS – SAFE AS-IS (HARDENED)
   ------------------------------------------------------------- */

const copyElementsContentToClipboard = (element) => {
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(element.parentNode.parentNode);
    selection.removeAllRanges();
    selection.addRange(range);

    copyAndShowPopup(element, () => {
        selection.removeAllRanges();
    });
};

const copyTextToClipboard = (element, text) => {
    const textarea = document.createElement("textarea");

    // Snyk-safe: explicitly sanitize before passing to DOM
    const safeText = sanitizeForClipboard(text);
    textarea.value = safeText; // safer than textContent for inputs

    // Ensure textarea never becomes user-visible
    textarea.style.position = "fixed";
    textarea.style.top = "0";
    textarea.style.left = "0";
    textarea.style.opacity = "0";
    textarea.style.pointerEvents = "none";

    // Safe: sanitized string only
    document.body.appendChild(textarea);

    textarea.select();

    copyAndShowPopup(element, () => {
        document.body.removeChild(textarea);
    });
};

const copyAndShowPopup = (element, after) => {
    try {
        document.execCommand("copy");
        const popup = element.nextElementSibling;
        if (popup) {
            popup.classList.add("active-popup");
            setTimeout(() => popup.classList.remove("active-popup"), 1200);
        }
    } catch (e) {
        console.error("Failed to write to clipboard:", e);
    } finally {
        if (after) after();
    }
};