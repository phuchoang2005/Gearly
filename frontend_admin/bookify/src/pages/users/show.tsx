// src/pages/users/show.tsx

import React from "react";
import { useShow, useNavigation } from "@refinedev/core";
import { Flex, Grid } from "antd";
import type { IUser } from "../../interfaces";
import { Drawer } from "../../components";
import {
  UserInfoSummary,
  UserInfoList,
  UserOrderHistory,
} from "../../components/user"; // adjust paths/names as needed

export const UserShow: React.FC = () => {
  const { list } = useNavigation();
  const breakpoint = Grid.useBreakpoint();

  // fetch from the "users" resource by id from URL
  const { query: queryResult, refetch } = useShow<IUser>({
    resource: "users",
  });

  const user = queryResult.data?.data;

  return (
    <Drawer
      open
      onClose={() => list("users")}
      width={breakpoint.sm ? 736 : "100%"}
    >
      <Flex vertical gap={32} style={{ padding: 32 }}>
        <UserInfoSummary user={user} refetch={queryResult.refetch!} />
        <UserInfoList user={user} />
        <UserOrderHistory user={user} />
      </Flex>
    </Drawer>
  );
};
