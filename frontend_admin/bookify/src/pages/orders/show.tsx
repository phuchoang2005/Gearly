import { useShow, useTranslate, useApiUrl, useNotification, useInvalidate } from "@refinedev/core";
import { IOrder } from "../../interfaces";
import { List, ListButton } from "@refinedev/antd";
import {
  Button,
  Col,
  Descriptions,
  Divider,
  Flex,
  Row,
  Skeleton,
  Typography,
  Tag,
  Collapse,
} from "antd";
const { Panel } = Collapse;
import {
  CloseCircleOutlined,
  LeftOutlined,
  SyncOutlined,
  CheckCircleOutlined,
  RocketOutlined,
  DollarCircleOutlined,
} from "@ant-design/icons";
import {
  CardWithContent,
  OrderDeliveryMap,
  OrderDeliveryDetails,
  OrderItemsTable,
} from "../../components";

export const OrderShow = () => {
  const t = useTranslate();
  const { open } = useNotification();
  const apiUrl = useApiUrl();
  const invalidate = useInvalidate();
  const { query: queryResult } = useShow<IOrder>();
  const { data, isLoading } = queryResult;
  const record = data?.data;
  const id = record?.id;

  const lastTransaction = record?.payment?.transactions?.slice(-1)?.[0];

  const callAction = async (endpoint: string, successMsg: string, errorMsg: string) => {
    if (!id) return;
    try {
      const res = await fetch(`${apiUrl}/orders/${id}/${endpoint}`, { method: "POST" });
      if (!res.ok) throw new Error(await res.text());
      const result = await res.json();
      if (result === true) {
        open?.({ type: "success", message: t(successMsg) });
        invalidate({ resource: "orders", invalidates: ["all"] });
      } else {
        open?.({ type: "error", message: t(errorMsg) });
      }
    } catch (err: any) {
      open?.({ type: "error", message: err.message || t(errorMsg) });
    }
  };

  const status = record?.orderStatus;
  const actionButtons: React.ReactNode[] = [];

  if (status === "PENDING") {
    actionButtons.push(
        <Button key="process" icon={<SyncOutlined />} onClick={() => callAction("set-process", "orders.notifications.processed", "orders.notifications.processError")}>
          {t("buttons.process", "Process")}
        </Button>,
        <Button key="cancel" icon={<CloseCircleOutlined />} danger onClick={() => callAction("set-cancel", "orders.notifications.cancelSuccess", "orders.notifications.cancelError")}>
          {t("buttons.cancel", "Cancel")}
        </Button>
    );
  } else if (status === "PROCESSING") {
    actionButtons.push(
        <Button key="ship" icon={<RocketOutlined />} onClick={() => callAction("set-ship", "orders.notifications.shipSuccess", "orders.notifications.shipError")}>
          {t("buttons.ship", "Ship")}
        </Button>,
        <Button key="cancel" icon={<CloseCircleOutlined />} danger onClick={() => callAction("set-cancel", "orders.notifications.cancelSuccess", "orders.notifications.cancelError")}>
          {t("buttons.cancel", "Cancel")}
        </Button>
    );
  } else if (status === "SHIPPED") {
    actionButtons.push(
        <Button key="deliver" icon={<CheckCircleOutlined />} onClick={() => callAction("set-delivered", "orders.notifications.deliveredSuccess", "orders.notifications.deliveredError")}>
          {t("buttons.deliver", "Deliver")}
        </Button>
    );
  } else if (status === "DELIVERED") {
    actionButtons.push(
        <Button key="complete" icon={<CheckCircleOutlined />} onClick={() => callAction("set-complete", "orders.notifications.completeSuccess", "orders.notifications.completeError")}>
          {t("buttons.complete", "Complete")}
        </Button>,
        <Button key="initiate-refund" icon={<DollarCircleOutlined />} onClick={() => callAction("set-pending-refund", "orders.notifications.initiateRefundSuccess", "orders.notifications.initiateRefundError")}>
          {t("buttons.initiateRefund", "Initiate Refund")}
        </Button>
    );
    } else if (status === "PENDING_REFUND") {
        actionButtons.push(
            <Button key="refund" icon={<DollarCircleOutlined />} onClick={() => callAction("set-refund", "orders.notifications.refundSuccess", "orders.notifications.refundError")}>
                {t("buttons.completeRefund", "Complete Refund")}
            </Button>
        );
  }

  return (
      <>
        <Flex>
          <ListButton icon={<LeftOutlined />}>{t("orders.orders")}</ListButton>
        </Flex>
        <Divider />
        <List
            breadcrumb={false}
            title={
              isLoading ? (
                  <Skeleton.Input active style={{ width: "144px", minWidth: "144px", height: "28px" }} />
              ) : (
                  `${t("orders.titles.list")} #${record?.id}`
              )
            }
            headerButtons={actionButtons}
        >
          <Row gutter={[16, 16]}>
            <Col xl={15} lg={24} md={24} sm={24} xs={24}>
              <Flex gap={16} vertical>
                <CardWithContent
                    bodyStyles={{ height: "378px", overflow: "hidden", padding: 0 }}
                    title={t("orders.titles.deliveryMap")}
                >
                  <OrderDeliveryMap order={record} />
                </CardWithContent>
                <OrderItemsTable order={record} />
              </Flex>
            </Col>

            <Col xl={9} lg={24} md={24} sm={24} xs={24}>
              <CardWithContent
                  bodyStyles={{ padding: 0 }}
                  title={t("orders.titles.deliveryDetails")}
              >
                {record && <OrderDeliveryDetails order={record} />}
              </CardWithContent>

              <Collapse defaultActiveKey={[2]} accordion style={{ marginTop: 16 }}>
                <Panel header={t("orders.sections.paymentInfo", "Payment Info")} key="1">
                  <Descriptions column={1} layout="vertical" size="small" bordered>
                    <Descriptions.Item label="Method">
                      <Tag color="purple">{record?.payment?.method?.toUpperCase() ?? "-"}</Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Amount">
                      <Typography.Text strong type="success">${record?.totalAmount?.toFixed(2)}</Typography.Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="Transaction ID">
                      <Typography.Text code>{lastTransaction?.transactionId ?? "-"}</Typography.Text>
                    </Descriptions.Item>
                    <Descriptions.Item label="Transaction Amount">
                      ${lastTransaction?.amount?.toFixed(2) ?? "-"}
                    </Descriptions.Item>
                    <Descriptions.Item label="Transaction Time">
                      {lastTransaction?.createdAt ? new Date(lastTransaction.createdAt).toLocaleString() : "—"}
                    </Descriptions.Item>
                    <Descriptions.Item label="Status">
                      <Tag color={lastTransaction?.status === "SUCCESSFUL" ? "green" : lastTransaction?.status === "FAILED" ? "red" : "orange"}>
                        {lastTransaction?.status ?? "-"}
                      </Tag>
                    </Descriptions.Item>
                  </Descriptions>
                </Panel>
                <Panel header={t("orders.sections.statusInfo", "Status Info")} key="2">
                  <Descriptions column={1} layout="vertical" size="small" bordered>
                    <Descriptions.Item label="Order Status">
                      <Tag color={record?.orderStatus === "COMPLETED" ? "green" : record?.orderStatus === "CANCELLED" ? "red" : "blue"}>
                        {record?.orderStatus}
                      </Tag>
                    </Descriptions.Item>
                    <Descriptions.Item label="Created At">
                      {record?.addedAt ? new Date(record.addedAt).toLocaleString() : "—"}
                    </Descriptions.Item>
                    <Descriptions.Item label="Updated At">
                        {record?.updatedAt ? new Date(record.updatedAt).toLocaleString() : "—"}
                    </Descriptions.Item>
                    <Descriptions.Item label="Completed At">
                      {record?.doneAt ? new Date(record.doneAt).toLocaleString() : "—"}
                    </Descriptions.Item>
                  </Descriptions>
                </Panel>
              </Collapse>
            </Col>
          </Row>
        </List>
      </>
  );
};
