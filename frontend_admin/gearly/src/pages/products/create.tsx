
// src/pages/products/create.tsx
import React from "react";
import { useGetToPath, useGo } from "@refinedev/core";
import { ProductDrawerForm } from "../../components/product/drawer-form";
import { useSearchParams } from "react-router";

export const ProductCreate: React.FC = () => {
    const getToPath = useGetToPath();
    const [searchParams] = useSearchParams();
    const go = useGo();

    return (
        <ProductDrawerForm
            action="create"
            onMutationSuccess={() => {
                const to =
                    searchParams.get("to") ??
                    getToPath({
                        // resource: "products",
                        action: "list"
                    }) ??
                    "";
                go({
                    to,
                    query: { to: undefined },
                    options: { keepQuery: true },
                    type: "replace",
                });
            }}
        />
    );
};
