import { useLocation, useNavigate } from 'react-router-dom';
import { strings } from '../strings.vi';
import { Modal } from './Modal';

interface SignInPromptProps {
  title: string;
  onClose: () => void;
}

/**
 * "Mời đăng nhập" dialog of W-AU08 for guests who try to write, vote or report (US4-AS5, US5-AS5). Both buttons
 * carry `?redirect=` so spec 010 brings the user back to this page.
 */
export function SignInPrompt({ title, onClose }: SignInPromptProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const redirect = encodeURIComponent(location.pathname + location.search);

  return (
    <Modal label={strings.signIn.label} title={title} onClose={onClose}>
      <p className="wk-muted">{strings.signIn.body}</p>
      <div className="wk-dialog__actions" style={{ justifyContent: 'stretch' }}>
        <button className="wk-btn wk-btn--ghost" type="button" onClick={onClose}>
          {strings.signIn.later}
        </button>
        <span className="sx-grow" />
        <button className="wk-btn wk-btn--secondary" type="button" onClick={() => navigate(`/register?redirect=${redirect}`)}>
          {strings.signIn.register}
        </button>
        <button className="wk-btn wk-btn--primary" type="button" onClick={() => navigate(`/login?redirect=${redirect}`)}>
          {strings.signIn.signIn}
        </button>
      </div>
    </Modal>
  );
}
