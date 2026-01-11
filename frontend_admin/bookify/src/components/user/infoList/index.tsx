import React from "react";
import type { IUser } from "../../../interfaces";
import {
    PhoneOutlined,
    EnvironmentOutlined,
    CheckCircleOutlined,
    UserOutlined,
    CalendarOutlined,
} from "@ant-design/icons";
import { List, Typography, theme, Card } from "antd";
import dayjs from "dayjs";
import { useTranslate } from "@refinedev/core";

type Props = {
    user?: IUser;
};

export const UserInfoList: React.FC<Props> = ({ user }) => {
    const { token } = theme.useToken();
    const t = useTranslate();

    return (
        <Card
            bordered={false}
            styles={{ body: {
                padding: "0 16px"
                },
            }}
        >
            <List
                itemLayout="horizontal"
                dataSource={[
                    {
                        title: t("users.fields.phone", "Phone"),
                        icon: <PhoneOutlined />,
                        value: <Typography.Text>{user?.phone}</Typography.Text>,
                    },
                    {
                        title: t("users.fields.address", "Address"),
                        icon: <EnvironmentOutlined />,
                        value: (
                            <Typography.Text>
                                {user?.address
                                    ? `${user.address.street}, ${user.address.city}, ${user.address.state} ${user.address.postalCode}, ${user.address.country}`
                                    : "-"}
                            </Typography.Text>
                        ),
                    },
                    {
                        title: t("users.fields.role", "Role"),
                        icon: <UserOutlined />,
                        value: <Typography.Text>{user?.role}</Typography.Text>,
                    },
                    {
                        title: t("users.fields.verified", "Verified"),
                        icon: (
                            <CheckCircleOutlined
                                style={{
                                    color: user?.verified
                                        ? token.colorSuccess
                                        : token.colorTextTertiary,
                                }}
                            />
                        ),
                        value: (
                            <Typography.Text>
                                {user?.verified
                                    ? t("users.fields.verified.true", "Yes")
                                    : t("users.fields.verified.false", "No")}
                            </Typography.Text>
                        ),
                    },
                    {
                        title: t("users.fields.createdAt", "Created At"),
                        icon: <CalendarOutlined />,
                        value: (
                            <Typography.Text>
                                {user?.createdAt
                                    ? dayjs(user.createdAt).format("MMMM, YYYY HH:mm A")
                                    : "-"}
                            </Typography.Text>
                        ),
                    },
                ]}
                renderItem={(item) => (
                    <List.Item>
                        <List.Item.Meta
                            avatar={item.icon}
                            title={<Typography.Text type="secondary">{item.title}</Typography.Text>}
                            description={item.value}
                        />
                    </List.Item>
                )}
            />
        </Card>
    );
};
