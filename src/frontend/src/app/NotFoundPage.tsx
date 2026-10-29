import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <section className="space-y-ds-3">
      <h1>Không tìm thấy trang</h1>
      <Link to="/" className="text-primary underline">
        Về trang chính
      </Link>
    </section>
  );
}
