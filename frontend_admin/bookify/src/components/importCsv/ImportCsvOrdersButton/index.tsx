// src/components/ImportCsvOrdersButton.tsx
import React from "react";
import { Button, Upload, message } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import Papa from "papaparse";
import {
  useCreate,
  useDataProvider,
  useInvalidate,
  useNotification,
  useTranslate,
} from "@refinedev/core";
import type { IOrder } from "../../interfaces";

export const ImportCsvOrdersButton: React.FC = () => {
  const dataProvider = useDataProvider();
  const invalidate = useInvalidate();
  const { mutateAsync: createOne } = useCreate<IOrder>();
  const { open } = useNotification();
  const t = useTranslate();

  const handleFile = (file: File) => {
    Papa.parse(file, {
      header: true,
      skipEmptyLines: true,
      complete: async (results) => {
        const rows = results.data as Record<string, string>[];
        try {
          await Promise.all(
            rows.map((row) => {
              // parse the items JSON column
              let items: IOrder["items"] = [];
              try {
                items = JSON.parse(row.itemsJson);
              } catch {
                // fallback: empty
                items = [];
              }
              // build payload
              const payload: Partial<IOrder> = {
                userId: row.userId,
                orderStatus: row.orderStatus as any,
                totalAmount: parseFloat(row.totalAmount),
                payment: { method: row.paymentMethod },
                shippingInformation: {
                  firstName: row.shippingFirstName,
                  lastName: row.shippingLastName,
                  email: row.shippingEmail,
                  phoneNumber: row.shippingPhoneNumber,
                  address: {
                    street: row.shippingStreet,
                    city: row.shippingCity,
                    state: row.shippingState,
                    postalCode: row.shippingPostalCode,
                    country: row.shippingCountry,
                  },
                },
                items,
              };
              // return dataProvider.createOne({
              //   resource: "orders",
              //   variables: { values: payload },
              // });
              return createOne({
                resource: "orders",
                values: payload,
              });
            })
          );

          if (open) {
            open({
              type: "success",
              message: t(
                "notifications.importSuccess",
                "Orders imported successfully"
              ),
            });
          }
          invalidate({ resource: "orders", invalidates: ["list"] });
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

    return false; // prevent default upload
  };

  return (
    <Upload accept=".csv" showUploadList={false} beforeUpload={handleFile}>
      <Button icon={<UploadOutlined />}>
        {t("buttons.importOrdersCsv", "Import Orders CSV")}
      </Button>
    </Upload>
  );
};
