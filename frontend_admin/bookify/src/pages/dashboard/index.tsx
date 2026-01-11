import type React from "react"
import {useMemo, useState} from "react"
import {
    Row,
    Col,
    Card,
    Typography,
    Button,
    Dropdown,
    type MenuProps,
    Statistic,
    Progress,
    Avatar,
    List,
    Tag,
    Space,
    Tabs,
    Alert,
    Badge,
    theme
} from "antd"
import {useTranslation} from "react-i18next"
import {useApiUrl, useCustom} from "@refinedev/core"
import {Column, Bar, Area} from "@ant-design/plots"
import dayjs from "dayjs"

import {
    ShoppingOutlined,
    UserOutlined,
    DollarCircleOutlined,
    TrophyOutlined,
    WarningOutlined,
    RiseOutlined,
    BookOutlined,
    TeamOutlined,
    ShopOutlined,
    AlertOutlined,
    CalendarOutlined,
    BarChartOutlined,
    LineChartOutlined,
} from "@ant-design/icons"

import type {
    TopCategoryQuantityDTO,
    BestSellerDTO,
    LoyalCustomerDTO,
    TopAvgOrderValueUserDTO,
    BookInLowStockDTO,
} from "../../interfaces"

type DateFilter = "lastWeek" | "lastMonth" | "lastQuarter" | "lastYear"
const DATE_FILTERS: Record<DateFilter, { text: string; icon: React.ReactNode }> = {
    lastWeek: {text: "lastWeek", icon: <CalendarOutlined/>},
    lastMonth: {text: "lastMonth", icon: <CalendarOutlined/>},
    lastQuarter: {text: "lastQuarter", icon: <CalendarOutlined/>},
    lastYear: {text: "lastYear", icon: <CalendarOutlined/>},
}

export const DashboardPage: React.FC = () => {
    const {t} = useTranslation()
    const API_URL = useApiUrl()
    const [dateFilter, setDateFilter] = useState<DateFilter>("lastWeek")
    const [activeTab, setActiveTab] = useState("overview")

    // Current Theme
    const {token} = theme.useToken()
    const isDarkMode = token.colorBgContainer === "#141414" || token.colorBgContainer === "#1f1f1f"

    // Customer color palette
    const colors = {
        chartColors: [
            "#1677ff", // Primary blue
            "#52c41a", // Success green
            "#faad14", // Warning orange
            "#722ed1", // Purple
            "#13c2c2", // Cyan
            "#fa8c16", // Orange
            "#f5222d", // Red
        ],

        // Status colors
        success: isDarkMode ? "#49aa19" : "#52c41a",
        warning: isDarkMode ? "#d89614" : "#faad14",
        error: isDarkMode ? "#a61d24" : "#ff4d4f",
        primary: isDarkMode ? "#177ddc" : "#1677ff",

        // Text colors
        textPrimary: token.colorText,
        textSecondary: token.colorTextSecondary,
        textTertiary: token.colorTextTertiary,

        // Background colors
        cardBg: token.colorBgContainer,
        pageBg: token.colorBgLayout,
    }

    const dateQuery = useMemo(() => {
        const now = dayjs()
        switch (dateFilter) {
            case "lastWeek":
                return {
                    start: now.subtract(6, "days").startOf("day").toISOString(),
                    end: now.endOf("day").toISOString(),
                }
            case "lastMonth":
                return {
                    start: now.subtract(1, "month").startOf("day").toISOString(),
                    end: now.endOf("day").toISOString(),
                }
            case "lastQuarter":
                return {
                    start: now.subtract(3, "months").startOf("day").toISOString(),
                    end: now.endOf("day").toISOString(),
                }
            case "lastYear":
                return {
                    start: now.subtract(1, "year").startOf("day").toISOString(),
                    end: now.endOf("day").toISOString(),
                }
        }
    }, [dateFilter])

    const menuItems: MenuProps["items"] = Object.entries(DATE_FILTERS).map(([key, {text, icon}]) => ({
        key,
        label: (
            <Space>
                {icon}
                {t(`dashboard.filter.date.${text}`)}
            </Space>
        ),
        onClick: () => setDateFilter(key as DateFilter),
    }))

    // API calls
    const {data: catData, isLoading: catLoading} = useCustom<{ data: TopCategoryQuantityDTO[] }>({
        url: `${API_URL}/dashboard/top-by-category`,
        method: "get",
        config: {query: dateQuery},
    })

    const {data: topBooksData, isLoading: booksLoading} = useCustom<{ data: BestSellerDTO[] }>({
        url: `${API_URL}/dashboard/top-books`,
        method: "get",
        config: {query: dateQuery},
    })

    const {data: loyalData, isLoading: loyalLoading} = useCustom<{ data: LoyalCustomerDTO[] }>({
        url: `${API_URL}/dashboard/loyal-customers`,
        method: "get",
        config: {query: dateQuery},
    })

    const {data: avgUserData, isLoading: avgUserLoading} = useCustom<{
        data: TopAvgOrderValueUserDTO
    }>({
        url: `${API_URL}/dashboard/top-user-by-avg-order-value`,
        method: "get",
        config: {query: dateQuery},
    })

    const {data: lowStockData, isLoading: stockLoading} = useCustom<{ data: BookInLowStockDTO[] }>({
        url: `${API_URL}/dashboard/book-in-low-stock`,
        method: "get",
        config: {query: dateQuery},
    })

    // Chart data processing
    const catChartData = useMemo(
        () =>
            catData?.data.map((c) => ({
                category: c.categoryName,
                total: c.top10Books.reduce((sum, b) => sum + b.totalQuantitySold, 0),
                books: c.top10Books.length,
            })) || [],
        [catData],
    )

    const bookChartData = useMemo(
        () =>
            topBooksData?.data.map((b) => ({
                title: b.title.length > 15 ? b.title.slice(0, 15) + "…" : b.title,
                sold: b.totalSold,
                revenue: b.totalSold * b.price,
                price: b.price,
            })) || [],
        [topBooksData],
    )

    const loyalChartData = useMemo(
        () =>
            loyalData?.data.map((u) => ({
                name: u.fullName.split(" ")[0], // First name only for chart
                fullName: u.fullName,
                orders: u.totalOrders,
                spending: u.totalSpending,
            })) || [],
        [loyalData],
    )

    // Calculate summary metrics
    const totalRevenue = useMemo(
        () => topBooksData?.data.reduce((sum, book) => sum + book.totalSold * book.price, 0) || 0,
        [topBooksData],
    )

    const totalBooksSold = useMemo(
        () => topBooksData?.data.reduce((sum, book) => sum + book.totalSold, 0) || 0,
        [topBooksData],
    )

    const criticalStockCount = useMemo(
        () => lowStockData?.data.length || 0,
        [lowStockData],
    )

    // Chart configurations
    const categoryBarConfig = {
        data: catChartData,
        xField: "total",
        yField: "category",
        seriesField: "category",
        legend: false,
        height: 280,
        color: colors.chartColors,
        barStyle: {
            radius: [0, 4, 4, 0],
        },
        label: {
            position: "right" as const,
            offset: 4,
            style: {
                fill: colors.textSecondary,
                fontSize: 12,
            },
        },
        xAxis: {
            grid: {
                line: {
                    style: {
                        stroke: token.colorBorderSecondary,
                        lineWidth: 1,
                    },
                },
            },
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
        yAxis: {
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
    }

    const bookColumnConfig = {
        data: bookChartData,
        xField: "title",
        yField: "sold",
        height: 280,
        color: colors.success,
        columnStyle: {
            radius: [4, 4, 0, 0],
        },
        label: {
            position: "top" as const,
            style: {
                fill: colors.textSecondary,
                fontSize: 12,
            },
        },
        xAxis: {
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
        yAxis: {
            grid: {
                line: {
                    style: {
                        stroke: token.colorBorderSecondary,
                        lineWidth: 1,
                    },
                },
            },
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
    }

    const loyalCustomerConfig = {
        data: loyalChartData,
        xField: "name",
        yField: "orders",
        height: 280,
        color: "#722ed1",
        columnStyle: {
            radius: [4, 4, 0, 0],
        },
        xAxis: {
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
        yAxis: {
            grid: {
                line: {
                    style: {
                        stroke: token.colorBorderSecondary,
                        lineWidth: 1,
                    },
                },
            },
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
    }

    const revenueAreaConfig = {
        data: bookChartData,
        xField: "title",
        yField: "revenue",
        height: 280,
        smooth: true,
        color: colors.warning,
        areaStyle: {
            fill: `l(270) 0:${colors.warning}10 0.5:${colors.warning}30 1:${colors.warning}50`,
        },
        xAxis: {
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
        yAxis: {
            grid: {
                line: {
                    style: {
                        stroke: token.colorBorderSecondary,
                        lineWidth: 1,
                    },
                },
            },
            label: {
                style: {
                    fill: colors.textSecondary,
                },
            },
        },
    }

    const cardStyle = {
        borderRadius: token.borderRadius,
        boxShadow: isDarkMode
            ? "0 2px 8px rgba(0, 0, 0, 0.15)"
            : "0 1px 2px rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px rgba(0, 0, 0, 0.02)",
    }

    const statCardStyle = {
        ...cardStyle,
        background: isDarkMode
            ? "linear-gradient(135deg, #1f1f1f 0%, #262626 100%)"
            : "linear-gradient(135deg, #ffffff 0%, #fafafa 100%)",
    }

    return (
        <div
            style={{
                padding: "24px",
                background: colors.pageBg,
                minHeight: "100vh",
                transition: "all 0.3s ease",
            }}
        >
            {/* Header */}
            <Row justify="space-between" align="middle" style={{marginBottom: 24}}>
                <Col>
                    <Typography.Title level={2} style={{margin: 0}}>
                        {t("dashboard.overview.title")}
                    </Typography.Title>
                    <Typography.Text style={{color: colors.textSecondary}}>
                        Comprehensive analytics and insights for Bookify
                    </Typography.Text>
                </Col>
                <Col>
                    <Space>
                        <Dropdown menu={{items: menuItems}} placement="bottomRight">
                            <Button type="primary" icon={<CalendarOutlined/>}>
                                {t(`dashboard.filter.date.${DATE_FILTERS[dateFilter].text}`)}
                            </Button>
                        </Dropdown>
                    </Space>
                </Col>
            </Row>

            {/* Key Metrics Cards */}
            <Row gutter={[16, 16]} style={{marginBottom: 24}}>
                <Col xs={24} sm={12} lg={6}>
                    <Card style={statCardStyle}>
                        <Statistic
                            title="Total Revenue"
                            value={totalRevenue}
                            precision={2}
                            valueStyle={{color: colors.success}}
                            prefix={<DollarCircleOutlined/>}
                            suffix="USD"
                        />
                        <div style={{marginTop: 8}}>
                            <Tag color="success" icon={<RiseOutlined/>}>
                                +12.5%
                            </Tag>
                            <Typography.Text style={{fontSize: 12, color: colors.textTertiary}}>vs last
                                period</Typography.Text>
                        </div>
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card style={statCardStyle}>
                        <Statistic
                            title="Books Sold"
                            value={totalBooksSold}
                            valueStyle={{color: colors.primary}}
                            prefix={<BookOutlined/>}
                        />
                        <div style={{marginTop: 8}}>
                            <Tag color="blue" icon={<RiseOutlined/>}>
                                +8.2%
                            </Tag>
                            <Typography.Text style={{fontSize: 12, color: colors.textTertiary}}>vs last
                                period</Typography.Text>
                        </div>
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card style={statCardStyle}>
                        <Statistic
                            title="Active Customers"
                            value={loyalData?.data.length || 0}
                            valueStyle={{color: "#722ed1"}}
                            prefix={<TeamOutlined/>}
                        />
                        <div style={{marginTop: 8}}>
                            <Tag color="purple" icon={<RiseOutlined/>}>
                                +5.1%
                            </Tag>
                            <Typography.Text style={{fontSize: 12, color: colors.textTertiary}}>vs last
                                period</Typography.Text>
                        </div>
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card style={statCardStyle}>
                        <Statistic
                            title="Low Stock Alert"
                            value={criticalStockCount}
                            valueStyle={{color: criticalStockCount > 0 ? colors.error : colors.success}}
                            prefix={<AlertOutlined/>}
                        />
                        <div style={{marginTop: 8}}>
                            {criticalStockCount > 0 ? (
                                <Tag color="error" icon={<WarningOutlined/>}>
                                    Action Required
                                </Tag>
                            ) : (
                                <Tag color="success">All Good</Tag>
                            )}
                        </div>
                    </Card>
                </Col>
            </Row>

            {/* Main Content Tabs */}
            <Card style={cardStyle}>
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    items={[
                        {
                            key: "overview",
                            label: (
                                <Space>
                                    <BarChartOutlined/>
                                    Overview
                                </Space>
                            ),
                            children: (
                                <Row gutter={[16, 16]}>
                                    {/* Top Categories */}
                                    <Col xs={24} lg={12}>
                                        <Card
                                            title={
                                                <Space>
                                                    <TrophyOutlined style={{color: colors.warning}}/>
                                                    {t("Top Categories Sold")}
                                                </Space>
                                            }
                                            loading={catLoading}
                                            style={cardStyle}
                                        >
                                            <Bar {...categoryBarConfig} />
                                        </Card>
                                    </Col>

                                    {/* Top Books */}
                                    <Col xs={24} lg={12}>
                                        <Card
                                            title={
                                                <Space>
                                                    <ShoppingOutlined style={{color: colors.success}}/>
                                                    {t("Best Selling Books")}
                                                </Space>
                                            }
                                            loading={booksLoading}
                                            style={cardStyle}
                                        >
                                            <Column {...bookColumnConfig} />
                                        </Card>
                                    </Col>

                                    {/* Loyal Customers */}
                                    <Col xs={24} lg={12}>
                                        <Card
                                            title={
                                                <Space>
                                                    <UserOutlined style={{color: "#722ed1"}}/>
                                                    {t("Loyal Customers")}
                                                </Space>
                                            }
                                            loading={loyalLoading}
                                            style={cardStyle}
                                        >
                                            <Column {...loyalCustomerConfig} />
                                        </Card>
                                    </Col>

                                    {/* Top Customer */}
                                    <Col xs={24} lg={12}>
                                        <Card
                                            title={
                                                <Space>
                                                    <DollarCircleOutlined style={{color: colors.primary}}/>
                                                    {t("Customer with Highest Avg Spend")}
                                                </Space>
                                            }
                                            loading={avgUserLoading}
                                            style={{...cardStyle, textAlign: "center"}}
                                        >
                                            <div style={{paddingTop: 60}}>
                                                <Avatar
                                                    size={80}
                                                    style={{
                                                        backgroundColor: colors.primary,
                                                        marginBottom: 16,
                                                    }}
                                                >
                                                    {avgUserData?.data.fullName?.charAt(0)}
                                                </Avatar>
                                                <Typography.Title level={3} style={{marginBottom: 8}}>
                                                    {avgUserData?.data.fullName}
                                                </Typography.Title>
                                                <Typography.Text style={{
                                                    display: "block",
                                                    marginBottom: 16,
                                                    color: colors.textSecondary
                                                }}>
                                                    {avgUserData?.data.email}
                                                </Typography.Text>
                                                <Statistic
                                                    title="Average Order Value"
                                                    value={avgUserData?.data.averageOrderValue || 0}
                                                    precision={2}
                                                    prefix="$"
                                                    valueStyle={{color: colors.primary, fontSize: 28}}
                                                />
                                            </div>
                                        </Card>
                                    </Col>
                                </Row>
                            ),
                        },
                        {
                            key: "sales",
                            label: (
                                <Space>
                                    <LineChartOutlined/>
                                    Sales Analytics
                                </Space>
                            ),
                            children: (
                                <Row gutter={[16, 16]}>
                                    <Col xs={24} lg={16}>
                                        <Card
                                            title={
                                                <Space>
                                                    <DollarCircleOutlined style={{color: colors.warning}}/>
                                                    Revenue Trend
                                                </Space>
                                            }
                                            style={cardStyle}
                                        >
                                            <Area {...revenueAreaConfig} />
                                        </Card>
                                    </Col>

                                    <Col xs={24} lg={8}>
                                        <Card title="Top Performers" style={cardStyle}>
                                            <List
                                                dataSource={topBooksData?.data.slice(0, 5)}
                                                renderItem={(book, index) => (
                                                    <List.Item>
                                                        <List.Item.Meta
                                                            avatar={
                                                                <Badge count={index + 1}
                                                                       style={{backgroundColor: colors.primary}}>
                                                                    <Avatar icon={<BookOutlined/>}/>
                                                                </Badge>
                                                            }
                                                            title={book.title}
                                                            description={
                                                                <Space direction="vertical" size={4}>
                                                                    <Typography.Text
                                                                        style={{color: colors.textSecondary}}>
                                                                        Sold: {book.totalSold} copies
                                                                    </Typography.Text>
                                                                    <Typography.Text strong
                                                                                     style={{color: colors.success}}>
                                                                        ${(book.totalSold * book.price).toFixed(2)}
                                                                    </Typography.Text>
                                                                </Space>
                                                            }
                                                        />
                                                    </List.Item>
                                                )}
                                            />
                                        </Card>
                                    </Col>
                                </Row>
                            ),
                        },
                        {
                            key: "customers",
                            label: (
                                <Space>
                                    <TeamOutlined/>
                                    Customer Insights
                                </Space>
                            ),
                            children: (
                                <Row gutter={[16, 16]}>
                                    <Col xs={24} lg={16}>
                                        <Card title="Customer Loyalty Analysis" style={cardStyle}>
                                            <Column {...loyalCustomerConfig} />
                                        </Card>
                                    </Col>

                                    <Col xs={24} lg={8}>
                                        <Card title="VIP Customers" style={cardStyle}>
                                            <List
                                                dataSource={loyalData?.data.slice(0, 5)}
                                                renderItem={(customer) => (
                                                    <List.Item>
                                                        <List.Item.Meta
                                                            avatar={
                                                                <Avatar
                                                                    style={{backgroundColor: "#722ed1"}}>{customer.fullName.charAt(0)}</Avatar>
                                                            }
                                                            title={customer.fullName}
                                                            description={
                                                                <Space direction="vertical" size={4}>
                                                                    <Typography.Text
                                                                        style={{color: colors.textSecondary}}>
                                                                        {customer.totalOrders} orders
                                                                    </Typography.Text>
                                                                    <Typography.Text strong
                                                                                     style={{color: colors.success}}>
                                                                        ${customer.totalSpending.toFixed(2)}
                                                                    </Typography.Text>
                                                                </Space>
                                                            }
                                                        />
                                                    </List.Item>
                                                )}
                                            />
                                        </Card>
                                    </Col>
                                </Row>
                            ),
                        },
                        {
                            key: "inventory",
                            label: (
                                <Space>
                                    <ShopOutlined/>
                                    Inventory
                                </Space>
                            ),
                            children: (
                                <Row gutter={[16, 16]}>
                                    <Col xs={24}>
                                        {criticalStockCount > 0 && (
                                            <Alert
                                                message="Low Stock Alert"
                                                description={`${criticalStockCount} books are running low on stock and need immediate attention.`}
                                                type="warning"
                                                showIcon
                                                style={{marginBottom: 16}}
                                            />
                                        )}

                                        <Card
                                            title={
                                                <Space>
                                                    <WarningOutlined style={{color: colors.warning}}/>
                                                    Books in Low Stock
                                                </Space>
                                            }
                                            loading={stockLoading}
                                            style={cardStyle}
                                        >
                                            <Row gutter={[16, 16]}>
                                                {lowStockData?.data.map((book) => (
                                                    <Col xs={24} sm={12} lg={8} xl={6} key={book.id}>
                                                        <Card
                                                            size="small"
                                                            style={{
                                                                textAlign: "center",
                                                                background:
                                                                    book.stock <= 3
                                                                        ? `${colors.error}15`
                                                                        : book.stock <= 5
                                                                            ? `${colors.warning}15`
                                                                            : `${colors.success}15`,
                                                                borderColor:
                                                                    book.stock <= 3
                                                                        ? `${colors.error}30`
                                                                        : book.stock <= 5
                                                                            ? `${colors.warning}30`
                                                                            : `${colors.success}30`,
                                                            }}
                                                        >
                                                            <Avatar
                                                                size={48}
                                                                icon={<BookOutlined/>}
                                                                style={{
                                                                    backgroundColor:
                                                                        book.stock <= 3 ? colors.error : book.stock <= 5 ? colors.warning : colors.success,
                                                                    marginBottom: 8,
                                                                }}
                                                            />
                                                            <Typography.Title level={5} style={{margin: "8px 0"}}>
                                                                {book.title}
                                                            </Typography.Title>
                                                            <Space direction="vertical" size={4}>
                                                                <Progress
                                                                    percent={(book.stock / 20) * 100}
                                                                    size="small"
                                                                    status={book.stock <= 3 ? "exception" : book.stock <= 5 ? "active" : "success"}
                                                                    showInfo={false}
                                                                />
                                                                <Typography.Text style={{color: colors.textSecondary}}>
                                                                    Stock: <Typography.Text
                                                                    strong>{book.stock}</Typography.Text>
                                                                </Typography.Text>
                                                                <Tag
                                                                    color={book.stock <= 3 ? "error" : book.stock <= 5 ? "warning" : "success"}>
                                                                    {book.stock <= 3 ? "Critical" : book.stock <= 5 ? "Low" : "Good"}
                                                                </Tag>
                                                            </Space>
                                                        </Card>
                                                    </Col>
                                                ))}
                                            </Row>
                                        </Card>
                                    </Col>
                                </Row>
                            ),
                        },
                    ]}
                />
            </Card>
        </div>
    )
}
