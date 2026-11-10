import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';
import { registerAiPlanner } from '@/features/ai-planner/register';
import '@/styles/index.css';
import '@/styles/soft-editorial.css';
import '@/styles/wikicook-web.css';

// Spec 031 plugs its "Tạo bằng AI" button and draft into the meal planner before the first render.
registerAiPlanner();

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
