/**
 * In-memory Storage for tests. Node 25+ defines its own global `localStorage`, which shadows happy-dom's and does
 * not store anything unless Node gets `--localstorage-file`.
 */
export function installMemoryStorage() {
  const data = new Map<string, string>();
  const storage: Storage = {
    get length() {
      return data.size;
    },
    clear: () => data.clear(),
    getItem: (key) => data.get(key) ?? null,
    key: (index) => [...data.keys()][index] ?? null,
    removeItem: (key) => {
      data.delete(key);
    },
    setItem: (key, value) => {
      data.set(key, String(value));
    },
  };
  Object.defineProperty(window, 'localStorage', { value: storage, configurable: true });
}
