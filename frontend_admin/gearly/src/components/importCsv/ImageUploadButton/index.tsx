import React from "react";
import { Upload, Button, message } from "antd";
import { UploadOutlined } from "@ant-design/icons";
import { useApiUrl, useNotification, useTranslate } from "@refinedev/core";

type ImageUploadButtonProps = {
  /** Called with the uploaded image URL on success */
  onUploadSuccess: (url: string) => void;
  /** Optional text for the button */
  buttonText?: string;
};

export const ImageUploadButton: React.FC<ImageUploadButtonProps> = ({
  onUploadSuccess,
  buttonText = "Upload Image",
}) => {
  const apiUrl = useApiUrl(); // e.g. http://localhost:8080/api
  const { open } = useNotification();
  const t = useTranslate();

  return (
    <Upload
      name="file"
      accept="image/*"
      showUploadList={false}
      action={`${apiUrl}/media/upload`}
      onChange={({ file }) => {
        if (file.status === "done") {
          // assume server responds { url: "/uploads/xxx.png" }
          const url = (file.response as any)?.url;
          if (url) {
            if (open) {
              open({
                type: "success",
                message: t("notifications.uploadSuccess", "Upload succeeded"),
              });
            }
            onUploadSuccess(url);
          } else {
            if (open) {
              open({
                type: "error",
                message: t("notifications.uploadError", "Upload failed"),
              });
            }
          }
        } else if (file.status === "error") {
          message.error(t("notifications.uploadError", "Upload error"));
        }
      }}
    >
      <Button icon={<UploadOutlined />}>{buttonText}</Button>
    </Upload>
  );
};
