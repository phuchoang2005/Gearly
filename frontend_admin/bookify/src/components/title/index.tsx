import {useLink} from "@refinedev/core";
import {Space, theme} from "antd";

import {Logo} from "./styled";

type TitleProps = {
    collapsed: boolean;
};

export const Title: React.FC<TitleProps> = ({collapsed}) => {
    const {token} = theme.useToken();
    const isDarkMode = token.colorBgContainer === "#141414" || token.colorBgContainer === "#1f1f1f"
    const Link = useLink();

    return (
        <Logo>
            <Link to="/">
                {collapsed ? isDarkMode ? (<img src={"/images/brand-logo-white.png"} width={140} height={60}/>) : (<img src={"/images/brand-logo-black.png"} width={140} height={60}/>)
                    : (
                    <Space size={12}>
                        {isDarkMode ? (<img src={"/images/brand-logo-white.png"} width={140} height={60}/>) : (<img src={"/images/brand-logo-black.png"} width={140} height={60}/>)}
                    </Space>
                )}
            </Link>
        </Logo>
    );
};
