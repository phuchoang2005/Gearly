import { useQuery } from "@tanstack/react-query";
import { getOrderByUser } from "@u_services/orderService.js";

export const useOrders = ({ pageIndex, status, search }) => {
    return useQuery({
        queryKey: ["orders", pageIndex, status, search],
        queryFn: () =>
            getOrderByUser({
                page: pageIndex,
                ...(status ? { status } : {}),
                ...(search ? { search } : {}),
            }).then((res) => res.data),
        refetchOnMount: "always"
    });
};
