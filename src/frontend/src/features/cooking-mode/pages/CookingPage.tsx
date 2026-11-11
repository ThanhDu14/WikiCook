import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { defaultRecipeReader, type RecipeReadResult } from '../adapters/recipeReader';
import { CookingView } from '../components/CookingView';
import type { RecipeSnapshot } from '../domain/sessionModel';

export function CookingPage(): React.ReactElement {
  const { recipeId } = useParams<{ recipeId: string }>();
  const navigate = useNavigate();
  const [snapshot, setSnapshot] = useState<RecipeSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [completed, setCompleted] = useState(false);

  useEffect(() => {
    let isCancelled = false;

    if (!recipeId) {
      // Keep route-load state synchronized when no recipe id is supplied.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setError('Thiếu mã công thức.');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError(null);
    setCompleted(false);
    defaultRecipeReader.loadPublished(recipeId).then((res: RecipeReadResult) => {
      if (isCancelled) return;
      setIsLoading(false);

      if (res.kind === 'READY') {
        setSnapshot(res.snapshot);
      } else if (res.kind === 'UNAVAILABLE') {
        setError('Công thức không tồn tại hoặc chưa xuất bản.');
      } else {
        setError('Không thể tải công thức nấu ăn. Vui lòng thử lại.');
      }
    });

    return () => {
      isCancelled = true;
    };
  }, [recipeId]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6 text-center">
        <p className="text-base font-semibold text-stone-500 animate-pulse">
          Đang tải chế độ nấu...
        </p>
      </div>
    );
  }

  if (error || !snapshot) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center p-6 text-center">
        <p className="text-lg font-bold text-red-600 mb-4">{error || 'Đã có lỗi xảy ra.'}</p>
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="rounded-xl bg-stone-200 px-4 py-2 font-semibold text-stone-700 hover:bg-stone-300 transition"
        >
          Quay lại
        </button>
      </div>
    );
  }

  if (completed) {
    // Local overview until the real 060 overview/070 handoff is available.
    return (
      <main className="mx-auto max-w-2xl p-6">
        <p role="status" className="rounded-xl bg-success-tint p-4 text-success">Chúc mừng bạn đã hoàn thành món ăn!</p>
        <h1 className="mt-6 text-2xl font-bold">{snapshot.title}</h1>
        <p className="mt-2">Đã hoàn thành {snapshot.steps.length} bước nấu.</p>
        <button className="mt-6 rounded-xl bg-primary px-4 py-3 text-primary-on" onClick={() => navigate('/shopping-list')}>Về danh sách mua sắm</button>
      </main>
    );
  }

  return (
    <CookingView
      key={snapshot.recipeId}
      initialSnapshot={snapshot}
      onClose={() => navigate('/shopping-list')}
      onFinish={() => setCompleted(true)}
    />
  );
}
