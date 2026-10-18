import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

// Order matters: Bootstrap first, then the theme that overrides it.
import 'bootstrap/dist/css/bootstrap.min.css';
import '@fortawesome/fontawesome-free/css/all.min.css';
import './theme/purple-black-theme.css';
import './styles/layout.css';
// Bootstrap's JS (with Popper) powers the data-bs-toggle dropdowns and the navbar collapse.
import 'bootstrap/dist/js/bootstrap.bundle.min.js';

import App from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
