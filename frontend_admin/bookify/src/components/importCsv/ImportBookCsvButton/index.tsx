// src/components/ImportBookCsvButton.tsx
import React from "react";
import { Button, Upload, message } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import Papa from "papaparse";
import {
  useCreate,
  useCreateMany,
  useDataProvider,
  useInvalidate,
  useNotification,
  useTranslate,
} from "@refinedev/core";
import { IBook } from "../../../interfaces";

export const ImportBookCsvButton: React.FC = () => {
  const dataProvider = useDataProvider();
  const { mutateAsync: createOne } = useCreate<IBook>();
  const invalidate = useInvalidate();
  const { open } = useNotification();
  const t = useTranslate();

  const handleFile = (file: File) => {
    Papa.parse(file, {
      header: true,
      skipEmptyLines: true,
      complete: async (results) => {
        const rows = results.data as Record<string, any>[];
        try {
          // create each book one‐by‐one
          await Promise.all(
            rows.map((row) => {
              console.log("row", row);
              const payload = {
                title: row.title,
                authors:
                  row.authors?.split(";").map((a: string) => a.trim()) || [],
                description: row.description,
                price: parseFloat(row.price),
                originalPrice: parseFloat(row.originalPrice),
                condition: row.condition,
                stock: parseInt(row.stock, 10),
                categoryIds:
                  row.categoryIds?.split(";").map((id: string) => id.trim()) ||
                  [],
                images: row.images
                  ? row.images
                      .split(";")
                      .map((url: string) => ({ url: url.trim() }))
                  : [],
              };
              // return dataProvider.create({
              //   resource: "books",
              //   values: payload,
              // });
              return createOne({
                resource: "books",
                values: payload,
              });
            })
          );

          if (open) {
            open({
              type: "success",
              message: t("notifications.importSuccess", "Import successful!"),
            });
          }
          // re‐fetch the list
          invalidate({ resource: "books", invalidates: ["list"] });
        } catch (err: any) {
          if (open) {
            open({
              type: "error",
              message:
                err.message || t("notifications.importError", "Import failed"),
            });
          }
        }
      },
      error: () => {
        message.error(t("notifications.csvParseError", "Failed to parse CSV"));
      },
    });
    // prevent Upload from doing its own upload
    return false;
  };

  return (
    <Upload accept=".csv" showUploadList={false} beforeUpload={handleFile}>
      <Button icon={<UploadOutlined />}>
        {t("buttons.importCsv", "Import CSV")}
      </Button>
    </Upload>
  );
};
