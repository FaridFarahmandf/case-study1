/*
 * Copyright 2014-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 license.
 */

window.addEventListener('load', () => {
    document.querySelectorAll('span.copy-icon').forEach(element => {
        element.addEventListener('click', () => copyElementsContentToClipboard(element));
    });

    document.querySelectorAll('span.anchor-icon').forEach(element => {
        element.addEventListener('click', () => {
            if (element.hasAttribute('pointing-to')) {
                const safeTarget = sanitizeFragment(element.getAttribute('pointing-to'));
                const location = safeBaseUrl() + '#' + safeTarget;
                copyTextToClipboard(element, location);
            }
        });
    });
});


/* -------------------------------------------------------------
    SAFE HELPERS
   ------------------------------------------------------------- */

/** Build a safe base URL (no query, no fragment, no attacker-controlled input) */
const safeBaseUrl = () => {
    const url = new URL(window.location.href);
    return url.origin + url.pathname;
};

/** Sanitize fragment identifiers to avoid DOM-XSS */
const sanitizeFragment = (value) => {
    if (!value) return '';
    return value.replace(/[^a-zA-Z0-9\-_.:]/g, '');
};


/* -------------------------------------------------------------
    EXISTING FUNCTIONS – SAFE AS-IS
   ------------------------------------------------------------- */

const copyElementsContentToClipboard = (element) => {
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(element.parentNode.parentNode);
    selection.removeAllRanges();
    selection.addRange(range);

    copyAndShowPopup(element, () => selection.removeAllRanges());
};

const copyTextToClipboard = (element, text) => {
    const textarea = document.createElement("textarea");

    // Safe: text is sanitized or controlled
    textarea.textContent = text;

    textarea.style.position = "fixed";
    textarea.style.opacity = "0";

    document.body.appendChild(textarea);
    textarea.select();

    copyAndShowPopup(element, () => document.body.removeChild(textarea));
};

const copyAndShowPopup = (element, after) => {
    try {
        document.execCommand('copy');
        element.nextElementSibling.classList.add('active-popup');
        setTimeout(() => {
            element.nextElementSibling.classList.remove('active-popup');
        }, 1200);
    } catch (e) {
        console.error('Failed to write to clipboard:', e);
    } finally {
        if (after) after();
    }
};
