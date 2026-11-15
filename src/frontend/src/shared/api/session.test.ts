import { GUEST, getAccessToken, getDevUserId, setAccessToken, setDevUserId } from './session';
import { installMemoryStorage } from './memoryStorage.testutil';

beforeAll(() => {
  installMemoryStorage();
});

describe('dev session (ACT-01)', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it('defaults to dev user 1 and no token', () => {
    expect(getDevUserId()).toBe(1);
    expect(getAccessToken()).toBeNull();
  });

  it('remembers the dev user and the guest choice', () => {
    setDevUserId(7);
    expect(getDevUserId()).toBe(7);

    setDevUserId(GUEST);
    expect(getDevUserId()).toBe(GUEST);
  });

  it('ignores a corrupt stored value', () => {
    window.localStorage.setItem('wk.devUserId', 'abc');
    expect(getDevUserId()).toBe(1);

    window.localStorage.setItem('wk.devUserId', '-4');
    expect(getDevUserId()).toBe(1);
  });

  it('stores and clears the access token', () => {
    setAccessToken('t1');
    expect(getAccessToken()).toBe('t1');

    setAccessToken(null);
    expect(getAccessToken()).toBeNull();
  });

  it('keeps the access token in memory only and drops the key an earlier build wrote (R2)', () => {
    window.localStorage.setItem('wk.accessToken', 'old');

    setAccessToken('t2');

    expect(window.localStorage.getItem('wk.accessToken')).toBeNull();
    expect(Object.keys(window.localStorage)).not.toContain('wk.accessToken');
    expect(getAccessToken()).toBe('t2');
    setAccessToken(null);
  });
});
