import {
  type BaseKey,
  type HttpError,
  useApiUrl,
  useGetToPath,
  useGo,
  useNavigation,
  useShow,
  useTranslate,
} from "@refinedev/core";
import {
  Avatar,
  Button,
  Divider,
  Flex,
  Grid,
  List,
  Typography,
  theme,
} from "antd";
import { useSearchParams } from "react-router";
import { Drawer } from "../../drawer";
import type { ICategory, IBook } from "../../../interfaces";
import { DeleteButton, NumberField } from "@refinedev/antd";
import { EditOutlined } from "@ant-design/icons";

type Props = {
  id?: BaseKey;
  onClose?: () => void;
  onEdit?: () => void;
};

export const BookDrawerShow: React.FC<Props> = ({
  id: propId,
  onClose,
  onEdit,
}) => {
  const getToPath = useGetToPath();
  const [searchParams] = useSearchParams();
  const go = useGo();
  const { editUrl } = useNavigation();
  const t = useTranslate();
  const { token } = theme.useToken();
  const breakpoint = Grid.useBreakpoint();

  const { query } = useShow<IBook, HttpError>({
    resource: "books",
    id: propId,
  });
  const book = query.data?.data;

  const handleDrawerClose = () => {
    if (onClose) return onClose();

    go({
      to: searchParams.get("to") ?? getToPath({ action: "list" }) ?? "",
      query: { to: undefined },
      options: { keepQuery: true },
      type: "replace",
    });
  };
  const apiUrl = useApiUrl(); // e.g. "http://localhost:8080/api"
  const backendOrigin = apiUrl.replace(/\/api$/, "");

  const imageUrl = book?.images?.[0]?.url
    ? `${backendOrigin}${book?.images[0].url}`
    : undefined;

  return (
    <Drawer
      open={true}
      width={breakpoint.sm ? "378px" : "100%"}
      zIndex={1001}
      onClose={handleDrawerClose}
    >
      <Flex vertical align="center" justify="center">
        <Avatar
          shape="square"
          style={{
            aspectRatio: 1,
            objectFit: "contain",
            width: "240px",
            height: "240px",
            margin: "16px auto",
            borderRadius: "8px",
          }}
          src={imageUrl}
          alt={book?.images?.[0]?.name}
        />
      </Flex>

      <Flex vertical style={{ backgroundColor: token.colorBgContainer }}>
        <Flex vertical style={{ padding: 16 }}>
          <Typography.Title level={5}>{book?.title}</Typography.Title>
          <Typography.Text type="secondary">
            {book?.description}
          </Typography.Text>
        </Flex>

        <Divider style={{ margin: 0, padding: 0 }} />

        <List
          dataSource={[
            {
              label: (
                <Typography.Text type="secondary">{t("Price")}</Typography.Text>
              ),
              value: (
                <NumberField
                  value={book?.price ?? 0}
                  options={{ style: "currency", currency: "USD" }}
                />
              ),
            },
            {
              label: (
                <Typography.Text type="secondary">
                  {t("Original Price")}
                </Typography.Text>
              ),
              value: (
                <NumberField
                  value={book?.originalPrice ?? 0}
                  options={{ style: "currency", currency: "USD" }}
                />
              ),
            },
            {
              label: (
                <Typography.Text type="secondary">
                  {t("Condition")}
                </Typography.Text>
              ),
              value: <Typography.Text>{book?.condition}</Typography.Text>,
            },
            {
              label: (
                <Typography.Text type="secondary">{t("Stock")}</Typography.Text>
              ),
              value: <Typography.Text>{book?.stock}</Typography.Text>,
            },
            {
              label: (
                <Typography.Text type="secondary">
                  {t("Authors")}
                </Typography.Text>
              ),
              value: (
                <Typography.Text>
                  {(book?.authors || []).join(", ")}
                </Typography.Text>
              ),
            },
            {
              label: (
                <Typography.Text type="secondary">
                  {t("Categories")}
                </Typography.Text>
              ),
              value: (
                <Typography.Text>
                  {(book?.categoryNames || []).join(", ")}
                </Typography.Text>
              ),
            },
            {
              label: (
                <Typography.Text type="secondary">
                  {t("Average Rating")}
                </Typography.Text>
              ),
              value: (
                <Typography.Text>
                  {book?.averageRating.toFixed(1)}
                </Typography.Text>
              ),
            },
            {
              label: (
                <Typography.Text type="secondary">
                  {t("Rating Count")}
                </Typography.Text>
              ),
              value: <Typography.Text>{book?.ratingCount}</Typography.Text>,
            },
          ]}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                style={{ padding: "0 16px" }}
                avatar={item.label}
                title={item.value}
              />
            </List.Item>
          )}
        />
      </Flex>

      <Flex
        align="center"
        justify="space-between"
        style={{ padding: "16px 16px 16px 0" }}
      >
        <DeleteButton
          type="text"
          recordItemId={book?.id}
          resource="admin/books"
          onSuccess={handleDrawerClose}
        />
        <Button
          icon={<EditOutlined />}
          onClick={() => {
            if (onEdit) return onEdit();
            return go({
              to: `${editUrl("books", book?.id ?? "")}`,
              query: { to: "/books" },
              options: { keepQuery: true },
              type: "replace",
            });
          }}
        >
          {t("actions.edit")}
        </Button>
      </Flex>
    </Drawer>
  );
};
