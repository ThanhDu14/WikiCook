import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';
import '@/styles/index.css';
import '@/styles/soft-editorial.css';
import '@/styles/wikicook-web.css';

async function prepareApp() {
  if (import.meta.env.DEV && import.meta.env.VITE_API_MOCK === 'true') {
    const { worker } = await import('./mocks/browser');
    await worker.start({ onUnhandledRequest: 'bypass' });
  }
}

prepareApp().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
});
