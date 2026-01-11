const colorMap = {
    NEW: 'bg-[#DB1515]',
    'LIKE NEW': 'bg-[#DB5415]',
    GOOD: 'bg-[#CE1B66]',
    ACCEPTABLE: 'bg-[#FFC107]',
};

const ConditionTag = ({ type, isFilter = false }) => (
    <span className={`${colorMap[type]} text-white font-semibold px-3 py-1 rounded shadow-xl ${isFilter ? 'w-18 text-center text-[8px]' : 'text-[10px]'}  h-5 drop-shadow-[0_3px_3px_rgba(0,0,0,0.4)]`}>
        {type}
    </span>
);

export default ConditionTag;
