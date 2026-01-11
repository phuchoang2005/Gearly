import { useQuery } from "@tanstack/react-query";
import { getOrderStats } from "@u_services/orderService.js";

export const useOrderStats = () => {
    return useQuery({
        queryKey: ["orderStats"],
        queryFn: () => getOrderStats().then((res) => res.data),
        refetchOnMount: "always",
    });
};
