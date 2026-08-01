import React from "react";
import type { IOrder } from "../../../interfaces";
import { Map, MapMarker } from "../../map";

type Props = {
    order?: IOrder;
};

export const OrderDeliveryMap: React.FC<Props> = ({ order }) => {
    // Demo: if the order has GPS coords on the shippingAddress, use them;
    // otherwise fall back to a fixed point
    const defaultCenter = { lat: 10.8231, lng: 106.6297 }; // Hanoi
    const shippingCoords = order?.shippingAddress?.coordinate;
    const center = shippingCoords
        ? { lat: shippingCoords[0], lng: shippingCoords[1] }
        : defaultCenter;

    return (
        <Map
            mapProps={{
                center,
                zoom: 12,
            }}
        >
            {/* Shipping address marker */}
            <MapMarker
                key="shipping-marker"
                icon={{ url: "/images/marker-shipping.svg" }}
                position={center}
            />

            {/* Demo-only second marker: offset a bit so you can see two */}
            <MapMarker
                key="demo-offset-marker"
                icon={{ url: "/images/marker-courier.svg" }}
                position={{
                    lat: center.lat + 0.01,
                    lng: center.lng + 0.01,
                }}
            />
        </Map>
    );
};
