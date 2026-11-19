import { getAccessToken, setAccessToken } from './session';
import { installMemoryStorage } from './memoryStorage.testutil';

beforeAll(() => {
  installMemoryStorage();
});

describe('session', () => {
  afterEach(() => {
    window.localStorage.clear();
    setAccessToken(null);
  });

  it('starts without a token', () => {
    expect(getAccessToken()).toBeNull();
  });

  it('stores and clears the access token', () => {
    setAccessToken('t1');
    expect(getAccessToken()).toBe('t1');

    setAccessToken(null);
    expect(getAccessToken()).toBeNull();
  });

  it('keeps the access token in memory only and drops the keys an earlier build wrote (R2)', () => {
    window.localStorage.setItem('wk.accessToken', 'old');
    window.localStorage.setItem('wk.devUserId', '7');

    setAccessToken('t2');

    expect(window.localStorage.getItem('wk.accessToken')).toBeNull();
    expect(window.localStorage.getItem('wk.devUserId')).toBeNull();
    expect(window.localStorage.length).toBe(0);
    expect(getAccessToken()).toBe('t2');
  });
});
