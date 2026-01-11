import { QueryClient } from '@tanstack/react-query';

const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 1000 * 60 * 5,
            cacheTime: 1000 * 60 * 10,
            retry: 2,
            refetchOnWindowFocus: false,
            refetchOnReconnect: true,
            refetchIntervalInBackground: false,
        },
    },
});

export default queryClient;
