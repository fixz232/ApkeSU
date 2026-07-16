const header = document.querySelector('[data-header]');
const sentinel = document.querySelector('[data-nav-sentinel]');
const menuButton = document.querySelector('[data-menu-button]');
const mobileMenu = document.querySelector('[data-mobile-menu]');
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

function setMenuOpen(open) {
  menuButton.setAttribute('aria-expanded', String(open));
  menuButton.setAttribute('aria-label', open ? '关闭导航菜单' : '打开导航菜单');
  mobileMenu.hidden = !open;
  header.classList.toggle('menu-open', open);
}

menuButton.addEventListener('click', () => {
  setMenuOpen(menuButton.getAttribute('aria-expanded') !== 'true');
});

mobileMenu.querySelectorAll('a').forEach((link) => {
  link.addEventListener('click', () => setMenuOpen(false));
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    setMenuOpen(false);
  }
});

const featureTabs = [...document.querySelectorAll('[data-feature-tab]')];
const featurePanels = [...document.querySelectorAll('[data-feature-panel]')];

function selectFeature(tab, moveFocus = false) {
  const target = tab.dataset.featureTab;
  featureTabs.forEach((item) => {
    const selected = item === tab;
    item.classList.toggle('is-active', selected);
    item.setAttribute('aria-selected', String(selected));
    item.tabIndex = selected ? 0 : -1;
  });
  featurePanels.forEach((panel) => {
    panel.hidden = panel.dataset.featurePanel !== target;
  });
  if (moveFocus) tab.focus();
}

featureTabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectFeature(tab));
  tab.addEventListener('keydown', (event) => {
    const previous = event.key === 'ArrowLeft' || event.key === 'ArrowUp';
    const next = event.key === 'ArrowRight' || event.key === 'ArrowDown';
    if (!previous && !next && event.key !== 'Home' && event.key !== 'End') return;
    event.preventDefault();
    const targetIndex = event.key === 'Home'
      ? 0
      : event.key === 'End'
        ? featureTabs.length - 1
        : (index + (next ? 1 : -1) + featureTabs.length) % featureTabs.length;
    selectFeature(featureTabs[targetIndex], true);
  });
});

const themeButtons = [...document.querySelectorAll('[data-theme-button]')];
const themePreview = document.querySelector('[data-theme-preview]');
const themeImage = document.querySelector('[data-theme-image]');
const themeName = document.querySelector('[data-theme-name]');
const themeTitle = document.querySelector('[data-theme-title]');
const themeDescription = document.querySelector('[data-theme-description]');

function selectTheme(button, moveFocus = false) {
  themeButtons.forEach((item) => {
    const selected = item === button;
    item.classList.toggle('is-active', selected);
    item.setAttribute('aria-selected', String(selected));
    item.tabIndex = selected ? 0 : -1;
  });

  themePreview.classList.add('is-changing');
  themePreview.dataset.themePreview = button.dataset.theme;
  themeImage.src = button.dataset.image;
  themeImage.alt = button.dataset.alt;
  themeName.textContent = button.dataset.name;
  themeTitle.textContent = button.dataset.title;
  themeDescription.textContent = button.dataset.description;
  window.setTimeout(() => themePreview.classList.remove('is-changing'), 220);
  if (moveFocus) button.focus();
}

themeButtons.forEach((button, index) => {
  button.addEventListener('click', () => selectTheme(button));
  button.addEventListener('keydown', (event) => {
    const previous = event.key === 'ArrowLeft' || event.key === 'ArrowUp';
    const next = event.key === 'ArrowRight' || event.key === 'ArrowDown';
    if (!previous && !next && event.key !== 'Home' && event.key !== 'End') return;
    event.preventDefault();
    const targetIndex = event.key === 'Home'
      ? 0
      : event.key === 'End'
        ? themeButtons.length - 1
        : (index + (next ? 1 : -1) + themeButtons.length) % themeButtons.length;
    selectTheme(themeButtons[targetIndex], true);
  });
});

if ('IntersectionObserver' in window) {
  const headerObserver = new IntersectionObserver(([entry]) => {
    header.classList.toggle('is-solid', !entry.isIntersecting);
  });
  headerObserver.observe(sentinel);
}

if (!reduceMotion && 'IntersectionObserver' in window) {
  document.body.classList.add('motion-ready');
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('is-visible');
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.12 });

  document.querySelectorAll('.reveal').forEach((element) => revealObserver.observe(element));
}
