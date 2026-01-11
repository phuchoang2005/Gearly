
// src/pages/books/create.tsx
import React from "react";
import { useGetToPath, useGo } from "@refinedev/core";
import { BookDrawerForm } from "../../components/book/drawer-form";
import { useSearchParams } from "react-router";

export const BookCreate: React.FC = () => {
    const getToPath = useGetToPath();
    const [searchParams] = useSearchParams();
    const go = useGo();

    return (
        <BookDrawerForm
            action="create"
            onMutationSuccess={() => {
                const to =
                    searchParams.get("to") ??
                    getToPath({
                        // resource: "books",
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
