import { useEffect } from 'react';

/**
 * Hook to keep both frontend and backend services alive on Render free tier
 * Pings every 10 minutes to prevent sleep after 15 minutes of inactivity
 */
export const useKeepAlive = () => {
  useEffect(() => {
    const backendUrl = import.meta.env.VITE_API_BASE_URL;
    
    const pingServices = async () => {
      // Ping backend health endpoint
      if (backendUrl) {
        try {
          await fetch(`${backendUrl}/api/health`, { 
            method: 'GET',
            cache: 'no-cache'
          });
          console.log('[KeepAlive] Backend ping successful');
        } catch (error) {
          console.error('[KeepAlive] Backend ping failed:', error);
        }
      }
      
      // Ping frontend (self) - just accessing any static resource
      try {
        await fetch(window.location.origin, { 
          method: 'HEAD',
          cache: 'no-cache'
        });
        console.log('[KeepAlive] Frontend ping successful');
      } catch (error) {
        console.error('[KeepAlive] Frontend ping failed:', error);
      }
    };

    // Initial ping after 1 minute
    const initialTimeout = setTimeout(pingServices, 60000);

    // Ping every 10 minutes (600,000 ms)
    const interval = setInterval(pingServices, 600000);

    return () => {
      clearTimeout(initialTimeout);
      clearInterval(interval);
    };
  }, []);
};
