import api from "../api.js";

export const fetchCountries = async () => {
    const res = await api.get("/addresses/countries");
    return res.data;
};

export const fetchStates = async (countryId) => {
    if (!countryId) return [];
    const res = await api.get("/addresses/states", {
        params: { countryId },
    });
    return res.data;
};

export const fetchCities = async ({ countryId, stateId }) => {
    if (!countryId || !stateId) return [];
    const res = await api.get("/addresses/cities", {
        params: { countryId, stateId },
    });
    return res.data;
};
