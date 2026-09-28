import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Runs an async loader and tracks loading, error and data.
 *
 * Every data-driven screen needs the same four things — loading, success, error and
 * empty — so they are handled once here instead of being re-implemented, and
 * inconsistently, on each page.
 *
 * `deps` controls re-fetching, exactly like a `useEffect` dependency list. The
 * latest request wins: a slow response from a previous filter can no longer
 * overwrite fresh results.
 */
export default function useAsync(loader, deps = [], { immediate = true } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState(null);

  const requestId = useRef(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const run = useCallback(async () => {
    const id = ++requestId.current;
    setLoading(true);
    setError(null);

    try {
      const result = await loaderRef.current();
      if (id === requestId.current) {
        setData(result);
      }
      return result;
    } catch (caught) {
      if (id === requestId.current) {
        setError(caught);
      }
      return null;
    } finally {
      if (id === requestId.current) {
        setLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (immediate) {
      run();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { data, loading, error, reload: run, setData };
}
