import { Link } from 'react-router-dom';

/** W-CM01 / CM01: 404 with the way back home and the four food tiles of the screens. */
export function NotFoundPage() {
  return (
    <section className="wx-split wx-split--even items-center py-ds-8" style={{ gap: 64 }}>
      <div className="sx-stack items-start" style={{ gap: 20 }}>
        <p className="wk-eyebrow">Lỗi 404</p>
        <h1 className="wk-display">Không tìm thấy trang</h1>
        <p className="wk-muted" style={{ maxWidth: '48ch' }}>
          Đường dẫn có thể sai hoặc trang đã bị xóa.
        </p>
        <div className="sx-row">
          <Link to="/" className="wk-btn wk-btn--primary">
            Về trang chủ
          </Link>
        </div>
      </div>
      <div className="wx-auth__tiles max-[900px]:!hidden" style={{ gridTemplateColumns: '1fr 1fr' }} aria-hidden="true">
        {['warm', 'saffron', 'cream', 'olive'].map((tone) => (
          <div key={tone} className={`wk-photo wk-photo--${tone}`}>
            <svg className="wk-icon" viewBox="0 0 24 24">
              <path d="M12 21a9 9 0 0 0 9-9H3a9 9 0 0 0 9 9Z" />
              <path d="M7 21h10" />
            </svg>
          </div>
        ))}
      </div>
    </section>
  );
}
