// import React from "react";
// import { Flex, Avatar, Typography } from "antd";
// import type { IUser } from "../../../interfaces";
//
// type Props = {
//     user?: IUser;
// };
//
// export const UserInfoSummary: React.FC<Props> = ({ user }) => {
//     // build initials fallback if no avatar URL is available
//     const initials = `${user?.firstName?.[0] ?? ""}${user?.lastName?.[0] ?? ""}`.toUpperCase();
//
//     return (
//         <Flex align="center" gap={32}>
//             <Avatar
//                 size={96}
//                 // If you later add an avatar URL field to IUser, swap in user.avatarUrl here.
//                 // Otherwise we show initials
//                 style={{ backgroundColor: "#87d068" }}
//             >
//                 {initials || "#"}
//             </Avatar>
//             <Flex vertical>
//                 <Typography.Text type="secondary">
//                     #{user?.id}
//                 </Typography.Text>
//                 <Typography.Title level={3} style={{ margin: 0 }}>
//                     {user?.fullName}
//                 </Typography.Title>
//             </Flex>
//         </Flex>
//     );
// };

import React from "react";
import { Flex, Avatar, Typography, Button, Space } from "antd";
import {
  useApiUrl,
  useInvalidate,
  useNotification,
  useTranslate,
} from "@refinedev/core";
import type { IUser } from "../../../interfaces";

type Props = {
  user?: IUser;
};

export const UserInfoSummary: React.FC<Props> = ({ user, refetch }) => {
  const apiUrl = useApiUrl(); // e.g. http://localhost:8080/api
  const invalidate = useInvalidate(); // to refresh the user list
  const { open } = useNotification(); // for toasts
  const t = useTranslate();

  // build initials fallback
  const initials = `${user?.firstName?.[0] ?? ""}${
    user?.lastName?.[0] ?? ""
  }`.toUpperCase();

  const callEndpoint = async (action: "activate" | "deactivate") => {
    if (!user?.id) return;
    try {
      const res = await fetch(`${apiUrl}/users/${user.id}/${action}`, {
        method: "POST",
      });
      if (!res.ok) {
        throw new Error(await res.text());
      }
      const ok: boolean = await res.json();
      if (ok) {
        if (open) {
          open({
            type: "success",
            message: t(`User ${action} Successfully`),
          });
        }
        await invalidate({
          resource: "users",
          invalidates: ["list", "show", "detail"],
        });
        refetch();
      } else {
        if (open) {
          open({
            type: "error",
            message: t(`User Activation Failed`),
          });
        }
      }
    } catch (err: any) {
      if (open) {
        open({
          type: "error",
          message: err.message || t(`users.notifications.${action}Error`),
        });
      }
    }
  };

  return (
    <Flex align="center" gap={32}>
      <Avatar size={96} style={{ backgroundColor: "#87d068" }}>
        {initials || "#"}
      </Avatar>
      <Flex vertical>
        <Typography.Text type="secondary">#{user?.id}</Typography.Text>
        <Typography.Title level={3} style={{ margin: 0 }}>
          {user?.fullName}
        </Typography.Title>

        <Space style={{ marginTop: 16 }}>
          <Button
            type="primary"
            onClick={() => callEndpoint("activate")}
            disabled={user?.status === "ACTIVE"}
          >
            {t("users.actions.activate", "Activate")}
          </Button>
          <Button
            danger
            onClick={() => callEndpoint("deactivate")}
            disabled={user?.status === "INACTIVE"}
          >
            {t("users.actions.deactivate", "Deactivate")}
          </Button>
        </Space>
      </Flex>
    </Flex>
  );
};
