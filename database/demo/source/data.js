// Dữ liệu demo: cây nhóm hàng và sản phẩm. Dùng bởi gen.js để sinh database/demo/*.sql
// Đơn vị: LON, LOC, THUNG, CHAI, GOI, HOP, KG, CAI có sẵn; còn lại thêm mới ở dưới.
const NEW_UNITS = [
  ["TUYP", "Tuýp"], ["TUI", "Túi"], ["BICH", "Bịch"], ["CUON", "Cuộn"], ["LO", "Lọ"],
  ["THANH", "Thanh"], ["BAO", "Bao"], ["CAN", "Can"], ["VI", "Vỉ"], ["LY", "Ly"],
];

// Cây nhóm hàng: { cấp 1: { cấp 2: [cấp 3...] } }
const TREE = {
  "Đồ uống": {
    "Nước giải khát": ["Nước ngọt có ga", "Nước tăng lực", "Nước trái cây", "Trà đóng chai", "Cà phê đóng chai"],
    "Bia": [], "Nước suối": [], "Nước uống thể thao": [], "Nước dừa và nước yến": [],
  },
  "Thực phẩm khô": {
    "Mì, phở ăn liền": ["Mì gói", "Mì ly và mì tô", "Phở và hủ tiếu ăn liền"],
    "Cháo ăn liền": [], "Bún, miến, phở khô": [], "Đồ hộp": [], "Ngũ cốc và bột": [],
  },
  "Bánh kẹo": {
    "Bánh quy": ["Bánh quy bơ và sữa", "Bánh quy kem"],
    "Bánh cracker": [], "Bánh xốp": [], "Kẹo cứng": [], "Kẹo mềm": [],
    "Chocolate": ["Chocolate thanh", "Chocolate viên"],
  },
  "Sữa và sản phẩm từ sữa": {
    "Sữa tươi": [], "Sữa tiệt trùng": [], "Sữa chua": [], "Sữa đặc": [], "Nước uống dinh dưỡng": [], "Sữa bột": [],
  },
  "Hóa mỹ phẩm": {
    "Dầu gội": ["Dầu gội nam", "Dầu gội nữ", "Dầu gội trị gàu"],
    "Dầu xả": [], "Sữa tắm": [], "Xà phòng": [], "Kem đánh răng": [], "Nước súc miệng": [],
  },
  "Chăm sóc gia đình": {
    "Nước giặt": ["Nước giặt cửa trước", "Nước giặt cửa trên"],
    "Nước xả vải": [], "Nước rửa chén": [], "Nước lau sàn": [], "Chất tẩy rửa": [], "Túi rác": [],
  },
  "Giấy và sản phẩm vệ sinh": {
    "Giấy vệ sinh": [], "Khăn giấy": [], "Khăn ướt": [], "Giấy ăn": [], "Băng vệ sinh": [],
  },
  "Đồ dùng cá nhân": {
    "Bàn chải đánh răng": [], "Dao cạo": [], "Khăn tắm và khăn mặt": [], "Vệ sinh cá nhân": [],
  },
  "Gia vị và nguyên liệu nấu ăn": {
    "Nước mắm": [], "Nước tương": [], "Dầu ăn": [], "Muối": [], "Đường": [], "Bột ngọt": [], "Hạt nêm": [], "Tương ớt": [],
  },
  "Đồ ăn nhẹ": {
    "Snack khoai và bắp": [], "Rong biển": [], "Hạt các loại": [], "Khô chế biến": [], "Đậu phộng": [], "Bắp rang": [],
  },
  "Cà phê và trà": {
    "Cà phê hòa tan": ["Cà phê 3 trong 1", "Cà phê đen và cappuccino"],
    "Cà phê rang xay": [], "Trà túi lọc": [], "Trà khô": [], "Trà hòa tan": [],
  },
  "Đồ dùng nhà bếp": {
    "Màng bọc thực phẩm": [], "Giấy bạc": [], "Túi zip": [], "Miếng rửa chén": [], "Găng tay": [], "Dụng cụ vệ sinh": [],
  },
  "Hàng tiêu dùng khác": { "Pin": [], "Bật lửa": [], "Khẩu trang": [], "Đồ dùng tiện ích": [] },
  "Thực phẩm đông lạnh": { "Xúc xích": [], "Chả cá": [], "Viên thả lẩu": [], "Thực phẩm chế biến sẵn": [] },
  "Đồ dùng cho trẻ em": { "Tã, bỉm": [], "Khăn ướt trẻ em": [], "Sữa trẻ em": [], "Đồ ăn dặm": [], "Chăm sóc trẻ em": [] },
  "Thức ăn thú cưng": { "Thức ăn cho chó": [], "Thức ăn cho mèo": [] },
};

const CAN = "LOC6,THUNG24"; // lon: lốc 6, thùng 24
const BOTTLE24 = "THUNG24";
const PACK30 = "THUNG30";

// Mỗi khối: [đường dẫn nhóm, ĐVT cơ sở, quy đổi mặc định, [[sku, tên, giá vốn, {u: ĐVT riêng, c: quy đổi riêng, x: ngừng KD}]...]]
const BLOCKS = [
  ["Đồ uống/Nước giải khát/Nước ngọt có ga", "LON", CAN, [
    ["7UP-330", "7UP lon 330ml", 6900], ["SPRITE-330", "Sprite lon 330ml", 7000], ["FANTA-CAM-330", "Fanta cam lon 330ml", 6900],
    ["MIRINDA-CAM-330", "Mirinda cam lon 330ml", 6800, { x: 1 }], ["MTNDEW-330", "Mountain Dew lon 330ml", 7000],
    ["SCHWEPPES-330", "Schweppes soda lon 330ml", 7600], ["COCA-ZERO-330", "Coca-Cola Zero lon 330ml", 7400],
    ["PEPSI-ZERO-330", "Pepsi không calo lon 330ml", 7200], ["COCA-390", "Coca-Cola chai 390ml", 5800, { u: "CHAI", c: BOTTLE24 }],
    ["COCA-1.5L", "Coca-Cola chai 1,5 lít", 17800, { u: "CHAI", c: "THUNG12" }], ["PEPSI-1.5L", "Pepsi chai 1,5 lít", 17500, { u: "CHAI", c: "THUNG12" }],
  ]],
  ["Đồ uống/Nước giải khát/Nước tăng lực", "CHAI", BOTTLE24, [
    ["STING-DAU-330", "Sting dâu chai 330ml", 6200], ["STING-VANG-330", "Sting vàng chai 330ml", 6200],
    ["NUMBER1-330", "Number 1 chai 330ml", 6500], ["WARRIOR-330", "Warrior dâu chai 330ml", 6300, { x: 1 }],
    ["MONSTER-355", "Monster Energy lon 355ml", 19500, { u: "LON", c: CAN }], ["BOHUC-250", "Bò húc Thái Lan lon 250ml", 12800, { u: "LON", c: CAN }],
  ]],
  ["Đồ uống/Nước giải khát/Nước trái cây", "CHAI", BOTTLE24, [
    ["TWISTER-CAM-455", "Tropicana Twister cam chai 455ml", 9800], ["NUTRI-CAM-297", "Nutriboost cam chai 297ml", 8800],
    ["VFRESH-CAM-1L", "Nước ép Vfresh cam hộp 1 lít", 24500, { u: "HOP", c: "THUNG12" }],
    ["DELMONTE-TAO-1L", "Nước ép Del Monte táo hộp 1 lít", 26500, { u: "HOP", c: "THUNG12" }],
    ["ZICO-DUA-330", "Nước dừa Zico hộp 330ml", 12500, { u: "HOP" }], ["TEPPY-CAM-327", "Nước cam ép Teppy chai 327ml", 6900],
  ]],
  ["Đồ uống/Nước giải khát/Trà đóng chai", "CHAI", BOTTLE24, [
    ["C2-XANH-455", "C2 trà xanh chai 455ml", 5400], ["TEAPLUS-OLONG-455", "Trà Ô Long Tea+ chai 455ml", 6900],
    ["O-DO-XANH-455", "Trà xanh Không Độ chai 455ml", 5200], ["LIPTON-CHANH-455", "Trà Lipton chanh chai 455ml", 5500],
    ["DRTHANH-500", "Trà thanh nhiệt Dr Thanh chai 500ml", 6000], ["WONDER-BIDAO-310", "Trà bí đao Wonderfarm hộp 310ml", 6400, { u: "HOP" }],
  ]],
  ["Đồ uống/Nước giải khát/Cà phê đóng chai", "CHAI", BOTTLE24, [
    ["HIGHLANDS-235", "Cà phê Highlands chai 235ml", 14500], ["NESCAFE-LON-170", "Cà phê sữa Nescafé lon 170ml", 9800, { u: "LON", c: CAN }],
    ["TRUECOFFEE-225", "Cà phê TH True Coffee chai 225ml", 11800], ["BIRDY-170", "Cà phê Birdy lon 170ml", 9500, { u: "LON", c: CAN }],
  ]],
  ["Đồ uống/Bia", "LON", CAN, [
    ["HEINEKEN-330", "Heineken lon 330ml", 18500], ["HEINEKEN-SILVER-330", "Heineken Silver lon 330ml", 18800],
    ["333-330", "Bia 333 lon 330ml", 9200], ["HANOI-330", "Bia Hà Nội lon 330ml", 9800], ["SAIGON-LAGER-330", "Bia Sài Gòn Lager lon 330ml", 10400],
    ["SAIGON-SPECIAL-330", "Bia Sài Gòn Special lon 330ml", 12800], ["SAPPORO-330", "Bia Sapporo Premium lon 330ml", 14500],
    ["HUDA-330", "Bia Huda lon 330ml", 9000], ["LARUE-330", "Bia Larue lon 330ml", 9300, { x: 1 }], ["BIVINA-330", "Bia Bivina lon 330ml", 9400],
    ["TIGER-CRYSTAL-330", "Tiger Crystal lon 330ml", 15200], ["TIGER-CHAI-330", "Bia Tiger chai 330ml", 14000, { u: "CHAI", c: BOTTLE24 }],
  ]],
  ["Đồ uống/Nước suối", "CHAI", BOTTLE24, [
    ["AQUAFINA-500", "Aquafina chai 500ml", 3200], ["AQUAFINA-1.5L", "Aquafina chai 1,5 lít", 5200, { c: "THUNG12" }],
    ["LAVIE-500", "Lavie chai 500ml", 3000], ["LAVIE-1.5L", "Lavie chai 1,5 lít", 4800, { c: "THUNG12" }],
    ["DASANI-500", "Dasani chai 500ml", 3100, { x: 1 }], ["VINHHAO-500", "Nước khoáng Vĩnh Hảo chai 500ml", 3400],
  ]],
  ["Đồ uống/Nước uống thể thao", "CHAI", BOTTLE24, [
    ["REVIVE-CHANHMUOI-500", "Revive chanh muối chai 500ml", 8800], ["REVIVE-500", "Revive vị tự nhiên chai 500ml", 8800],
    ["POCARI-350", "Pocari Sweat chai 350ml", 14500], ["AQUARIUS-390", "Aquarius chai 390ml", 8200],
  ]],
  ["Đồ uống/Nước dừa và nước yến", "HOP", BOTTLE24, [
    ["COCOXIM-330", "Nước dừa Cocoxim hộp 330ml", 10500], ["COCOLAND-330", "Nước dừa Cocoland hộp 330ml", 11500],
    ["SANEST-190", "Nước yến Sanest lon 190ml", 15500, { u: "LON", c: "LOC6,THUNG30" }],
  ]],

  ["Thực phẩm khô/Mì, phở ăn liền/Mì gói", "GOI", PACK30, [
    ["KOKOMI-TOM", "Mì Kokomi tôm chua cay 65g", 3500], ["OMACHI-SUON", "Mì Omachi sườn hầm ngũ quả", 5600], ["OMACHI-XOTBO", "Mì Omachi xốt bò hầm", 5600],
    ["GAUDO-GA", "Mì Gấu Đỏ gà", 2700], ["GAUDO-TOM", "Mì Gấu Đỏ tôm", 2700], ["3MIEN-GA", "Mì 3 Miền gà", 2500],
    ["HAOHAO-SATE", "Mì Hảo Hảo sa tế hành tím", 3300], ["HAOHAO-GA", "Mì Hảo Hảo gà", 3300], ["MODERN-LAUTHAI", "Mì Modern lẩu Thái", 3900],
    ["SIUKAY-SUON", "Mì Siukay sườn heo", 4000], ["CUNGDINH-HAI", "Mì Cung Đình hải sản", 5500], ["MAMA-TOMYUM", "Mì Mama Tomyum", 5200, { x: 1 }], ["VIFON-BO", "Mì Vifon bò hầm", 3600],
  ]],
  ["Thực phẩm khô/Mì, phở ăn liền/Mì ly và mì tô", "LY", "THUNG24", [
    ["HAOHAO-LY-TOM", "Mì ly Hảo Hảo tôm chua cay 67g", 6200], ["OMACHI-LY-SUON", "Mì ly Omachi sườn hầm", 8800], ["KOKOMI-LY-TOM", "Mì ly Kokomi tôm chua cay", 5800],
    ["CUNGDINH-LY", "Mì ly Cung Đình hải sản", 7200], ["HAOHAO-TO-TOM", "Mì tô Hảo Hảo tôm chua cay", 8500, { u: "CAI" }],
  ]],
  ["Thực phẩm khô/Mì, phở ăn liền/Phở và hủ tiếu ăn liền", "GOI", PACK30, [
    ["DENHAT-BOHAM", "Phở Đệ Nhất bò hầm", 6200], ["VIFON-PHOBO", "Phở Vifon bò tái", 6000], ["VIFON-HUTIEU", "Hủ tiếu Nam Vang Vifon", 6500],
    ["GAUDO-PHOGA", "Phở Gấu Đỏ gà", 5200],
  ]],
  ["Thực phẩm khô/Cháo ăn liền", "GOI", PACK30, [
    ["GAUDO-CHAO-THIT", "Cháo Gấu Đỏ thịt bằm", 4800], ["GAUDO-CHAO-GA", "Cháo Gấu Đỏ gà", 4800], ["CAYTHI-CHAO-GA", "Cháo Cây Thị gà", 5500],
    ["CAYTHI-CHAO-SUON", "Cháo Cây Thị sườn", 5500], ["VIFON-CHAO-THIT", "Cháo Vifon thịt bằm", 4600, { x: 1 }],
  ]],
  ["Thực phẩm khô/Bún, miến, phở khô", "GOI", "THUNG20", [
    ["SAFOCO-BUNGAO-300", "Bún gạo khô Safoco 300g", 9500], ["PHUHUONG-MIENDONG-200", "Miến dong Phú Hương 200g", 21000],
    ["BICHCHI-PHOKHO-500", "Phở khô Bích Chi 500g", 18500], ["BICHCHI-HUTIEU-500", "Hủ tiếu khô Bích Chi 500g", 19000],
    ["SAFOCO-NUI-400", "Nui ống Safoco 400g", 12500],
  ]],
  ["Thực phẩm khô/Đồ hộp", "HOP", "THUNG24", [
    ["HALONG-CAMOI-155", "Cá mòi sốt cà Hạ Long hộp 155g", 14500], ["HALONG-CANGU-140", "Cá ngừ ngâm dầu Hạ Long hộp 140g", 22000],
    ["COTDEN-PATE-170", "Pate Cột Đèn Hải Phòng hộp 170g", 19500], ["VISSAN-THITHAM-150", "Thịt heo hầm Vissan hộp 150g", 21500],
    ["SPAM-340", "Thịt hộp Spam 340g", 62000], ["DOHAN-DAUHALAN-400", "Đậu Hà Lan Dohan hộp 400g", 18000, { x: 1 }],
  ]],
  ["Thực phẩm khô/Ngũ cốc và bột", "BICH", "THUNG20", [
    ["MEIZAN-BOTMI-1KG", "Bột mì đa dụng Meizan 1kg", 21500], ["TAIKY-BOTCHIEN-150", "Bột chiên giòn Tài Ký 150g", 9500, { u: "GOI", c: "THUNG30" }],
    ["TAIKY-BOTNANG-400", "Bột năng Tài Ký 400g", 12500], ["QUAKER-YENMACH-400", "Yến mạch Quaker hộp 400g", 48000, { u: "HOP", c: "THUNG12" }],
    ["NESVITA-NGUCOC-400", "Ngũ cốc Nesvita bịch 400g", 58000, { c: "THUNG12" }],
  ]],

  ["Bánh kẹo/Bánh quy/Bánh quy bơ và sữa", "GOI", "THUNG24", [
    ["COSY-MARIE-144", "Bánh quy Cosy Marie 144g", 12500], ["COSY-BO-300", "Bánh quy bơ Cosy 300g", 24500], ["DANISA-BO-454", "Bánh quy bơ Danisa hộp thiếc 454g", 98000, { u: "HOP", c: "THUNG12" }],
    ["AFC-VANG-100", "Bánh AFC vị lúa mì 100g", 9500], ["KINHDO-BOSUA-138", "Bánh quy Kinh Đô bơ sữa 138g", 13500],
  ]],
  ["Bánh kẹo/Bánh quy/Bánh quy kem", "GOI", "THUNG24", [
    ["OREO-137", "Bánh Oreo kem vani 137g", 15500], ["OREO-DAUTAY-137", "Bánh Oreo kem dâu 137g", 15500], ["CHOCOPIE-12", "Bánh Choco Pie hộp 12 cái", 52000, { u: "HOP", c: "THUNG12" }],
    ["COSY-KEM-300", "Bánh quy kem Cosy 300g", 26500, { x: 1 }],
  ]],
  ["Bánh kẹo/Bánh cracker", "GOI", "THUNG24", [
    ["AFC-CRACKER-200", "Bánh cracker AFC 200g", 17500], ["RITZ-CRACKER-100", "Bánh cracker Ritz 100g", 18500], ["COSY-CRACKER-300", "Bánh cracker Cosy 300g", 24000],
    ["HAIHA-CRACKER-130", "Bánh cracker Hải Hà 130g", 12500],
  ]],
  ["Bánh kẹo/Bánh xốp", "GOI", "THUNG30", [
    ["WAFER-HANHNHAN-150", "Bánh xốp Hanh Nhân phủ socola 150g", 19000], ["JOJO-XOP-100", "Bánh xốp Jojo kem sữa 100g", 11500], ["TIPO-XOP-100", "Bánh xốp Tipo phô mai 100g", 12500],
    ["GOLDENWAFER-135", "Bánh xốp Golden Wafer cuộn 135g", 14500],
  ]],
  ["Bánh kẹo/Kẹo cứng", "BICH", "THUNG20", [
    ["ALPENLIEBE-CARAMEN-120", "Kẹo Alpenliebe caramen 120g", 14000], ["ALPENLIEBE-CAM-120", "Kẹo Alpenliebe cam 120g", 14000], ["HAIHA-KEODUA-200", "Kẹo dừa Hải Hà 200g", 15500],
    ["CHUPA-CHUPS-12", "Kẹo mút Chupa Chups túi 12 cây", 22000, { u: "TUI" }], ["SWING-BAHAHA-120", "Kẹo bạc hà Doublemint 100g", 13500, { x: 1 }],
  ]],
  ["Bánh kẹo/Kẹo mềm", "BICH", "THUNG20", [
    ["MENTOS-BACHA-37", "Kẹo Mentos bạc hà cuộn 37g", 6500, { u: "CUON", c: "THUNG60" }], ["MENTOS-TRAICAY-37", "Kẹo Mentos trái cây cuộn 37g", 6500, { u: "CUON", c: "THUNG60" }],
    ["HARIBO-GAUVANG-80", "Kẹo dẻo Haribo gấu vàng 80g", 24500, { u: "TUI", c: "THUNG24" }], ["CHEWY-SUA-100", "Kẹo sữa dẻo Chewy 100g", 12500], ["MAMEE-KEODEO-90", "Kẹo dẻo trái cây Mamee 90g", 11000],
  ]],
  ["Bánh kẹo/Chocolate/Chocolate thanh", "THANH", "THUNG48", [
    ["KITKAT-4F-35", "Chocolate KitKat 4 thanh 35g", 9500], ["SNICKERS-51", "Chocolate Snickers thanh 51g", 14500], ["MARS-51", "Chocolate Mars thanh 51g", 14500],
    ["TWIX-50", "Chocolate Twix thanh 50g", 14500], ["LOTTE-XYLI-40", "Chocolate Lotte Ghana 40g", 12500, { u: "THANH" }],
  ]],
  ["Bánh kẹo/Chocolate/Chocolate viên", "GOI", "THUNG24", [
    ["MM-DAUPHONG-40", "Chocolate M&M's đậu phộng 40g", 15500], ["MM-SOCOLA-40", "Chocolate M&M's sữa 40g", 15500], ["MALTESERS-37", "Chocolate Maltesers 37g", 18500],
    ["FERRERO-T16", "Chocolate Ferrero Rocher hộp 16 viên", 168000, { u: "HOP", c: "THUNG12" }],
  ]],

  ["Sữa và sản phẩm từ sữa/Sữa tươi", "HOP", "LOC4,THUNG48", [
    ["VINAMILK-TUOI-ITDUONG-180", "Sữa tươi Vinamilk 100% ít đường 180ml", 6200], ["VINAMILK-TUOI-KDUONG-180", "Sữa tươi Vinamilk 100% không đường 180ml", 6200],
    ["TH-TRUEMILK-180", "Sữa tươi TH True Milk 180ml", 7200], ["TH-TRUEMILK-ITDUONG-180", "Sữa tươi TH True Milk ít đường 180ml", 7200],
    ["DALAT-TUOI-180", "Sữa tươi Dalat Milk 180ml", 6800], ["VINAMILK-TUOI-1L", "Sữa tươi Vinamilk 100% hộp 1 lít", 31500, { c: "THUNG12" }],
  ]],
  ["Sữa và sản phẩm từ sữa/Sữa tiệt trùng", "HOP", "LOC4,THUNG48", [
    ["DUTCHLADY-DAU-180", "Sữa tiệt trùng Dutch Lady dâu 180ml", 5600], ["DUTCHLADY-SOCOLA-180", "Sữa tiệt trùng Dutch Lady socola 180ml", 5600],
    ["VINAMILK-FLEX-180", "Sữa tiệt trùng Vinamilk Flex 180ml", 5400], ["DELIMIL-DAU-180", "Sữa tiệt trùng Fami dâu 180ml", 5200], ["MILO-HOP-180", "Sữa Milo hộp 180ml", 6900, { x: 1 }],
  ]],
  ["Sữa và sản phẩm từ sữa/Sữa chua", "HOP", "LOC4,THUNG48", [
    ["VINAMILK-SC-CODUONG-100", "Sữa chua ăn Vinamilk có đường hộp 100g", 5400], ["VINAMILK-SC-TRAICAY-100", "Sữa chua trái cây Vinamilk hộp 100g", 6200],
    ["TH-SC-100", "Sữa chua TH True Yogurt hộp 100g", 7200], ["PROBI-65", "Sữa chua uống Probi chai 65ml", 3800, { u: "CHAI", c: "LOC5,THUNG60" }],
    ["YAKULT-65", "Sữa chua uống Yakult chai 65ml", 3500, { u: "CHAI", c: "LOC5,THUNG50" }],
  ]],
  ["Sữa và sản phẩm từ sữa/Sữa đặc", "HOP", "THUNG48", [
    ["ONGTHO-380", "Sữa đặc Ông Thọ hộp 380g", 24500], ["NGOISAO-380", "Sữa đặc Ngôi Sao Phương Nam hộp 380g", 23500], ["VINAMILK-DACTUOI-380", "Sữa đặc Vinamilk hộp 380g", 25500],
    ["TUI-ONGTHO-545", "Sữa đặc Ông Thọ túi 545g", 32500, { u: "TUI", c: "THUNG24" }],
  ]],
  ["Sữa và sản phẩm từ sữa/Nước uống dinh dưỡng", "HOP", "LOC4,THUNG48", [
    ["ENSURE-LON-237", "Sữa Ensure Gold lon 237ml", 38500, { u: "LON", c: "LOC6,THUNG24" }], ["VINAMILK-SUSU-180", "Sữa Susu Vinamilk 180ml", 5200], ["SUSU-DAU-110", "Sữa Susu dâu 110ml", 4300, { c: "LOC4,THUNG48" }],
    ["SUPERNUT-180", "Sữa hạt Vinamilk Super Nut 180ml", 8500], ["OVALTINE-180", "Thức uống lúa mạch Ovaltine 180ml", 6400],
  ]],
  ["Sữa và sản phẩm từ sữa/Sữa bột", "HOP", "THUNG12", [
    ["ENSURE-GOLD-850", "Sữa bột Ensure Gold 850g", 790000, { c: "THUNG6" }], ["SURE-PREVENT-900", "Sữa bột Sure Prevent 900g", 560000, { c: "THUNG6" }],
    ["VINAMILK-CANXI-400", "Sữa bột Vinamilk Canxi 400g", 125000], ["DIELAC-ALPHA-900", "Sữa bột Dielac Alpha Gold 900g", 245000, { c: "THUNG6" }],
  ]],

  ["Hóa mỹ phẩm/Dầu gội/Dầu gội nam", "CHAI", "THUNG12", [
    ["CLEARMEN-650", "Dầu gội Clear Men sạch gàu 650g", 135000], ["ROMANO-CLASSIC-650", "Dầu gội Romano Classic 650g", 98000], ["DOVEMEN-CARE-640", "Dầu gội Dove Men+Care 640g", 142000],
  ]],
  ["Hóa mỹ phẩm/Dầu gội/Dầu gội nữ", "CHAI", "THUNG12", [
    ["SUNSILK-MEMUOT-650", "Dầu gội Sunsilk mềm mượt diệu kỳ 650g", 118000], ["DOVE-PHUCHOI-640", "Dầu gội Dove phục hồi hư tổn 640g", 138000],
    ["TRESEMME-VAINEP-640", "Dầu gội TRESemmé vào nếp 640g", 132000], ["PANTENE-TOCDEP-650", "Dầu gội Pantene tóc dài óng ả 650g", 126000, { x: 1 }],
  ]],
  ["Hóa mỹ phẩm/Dầu gội/Dầu gội trị gàu", "CHAI", "THUNG12", [
    ["CLEAR-BACHA-630", "Dầu gội Clear bạc hà mát lạnh 630g", 128000], ["HEADSHOULDERS-TUOIMAT-625", "Dầu gội Head & Shoulders tươi mát 625ml", 134000],
    ["NIZORAL-100", "Dầu gội Nizoral trị gàu 100ml", 165000, { c: "THUNG24" }],
  ]],
  ["Hóa mỹ phẩm/Dầu xả", "CHAI", "THUNG12", [
    ["SUNSILK-DAUXA-320", "Dầu xả Sunsilk mềm mượt 320g", 62000], ["DOVE-DAUXA-335", "Dầu xả Dove dưỡng chất 335g", 72000], ["TRESEMME-DAUXA-340", "Dầu xả TRESemmé vào nếp 340g", 78000],
    ["CLEAR-DAUXA-300", "Dầu xả Clear sạch gàu 300g", 65000],
  ]],
  ["Hóa mỹ phẩm/Sữa tắm", "CHAI", "THUNG12", [
    ["DOVE-SUATAM-530", "Sữa tắm Dove dưỡng ẩm 530g", 142000], ["LIFEBUOY-SUATAM-850", "Sữa tắm Lifebuoy bảo vệ vượt trội 850g", 118000], ["LUX-SUATAM-800", "Sữa tắm Lux hoa hồng 800g", 112000],
    ["PALMOLIVE-SUATAM-750", "Sữa tắm Palmolive thiên nhiên 750ml", 98000], ["ENCHANTEUR-SUATAM-650", "Sữa tắm Enchanteur hương nước hoa 650g", 125000, { x: 1 }],
  ]],
  ["Hóa mỹ phẩm/Xà phòng", "CAI", "THUNG72", [
    ["LIFEBUOY-XP-90", "Xà phòng Lifebuoy bánh 90g", 8500], ["LUX-XP-90", "Xà phòng Lux bánh 90g", 8800], ["DOVE-XP-90", "Xà phòng Dove bánh 90g", 14500], ["CAMAY-XP-90", "Xà phòng Camay bánh 90g", 9500],
  ]],
  ["Hóa mỹ phẩm/Kem đánh răng", "TUYP", "THUNG48", [
    ["PS-180", "Kem đánh răng P/S bảo vệ 123 tuýp 180g", 28500], ["PS-MUOI-180", "Kem đánh răng P/S muối tuýp 180g", 29500], ["COLGATE-MAXFRESH-200", "Kem đánh răng Colgate Maxfresh tuýp 200g", 38500],
    ["CLOSEUP-HOA-160", "Kem đánh răng Closeup nụ hoa tuýp 160g", 33500], ["SENSODYNE-100", "Kem đánh răng Sensodyne tuýp 100g", 68000, { c: "THUNG24" }],
  ]],
  ["Hóa mỹ phẩm/Nước súc miệng", "CHAI", "THUNG12", [
    ["LISTERINE-COOLMINT-750", "Nước súc miệng Listerine Coolmint 750ml", 112000], ["COLGATE-PLAX-500", "Nước súc miệng Colgate Plax 500ml", 68000], ["PS-SUCMIENG-250", "Nước súc miệng P/S 250ml", 38500, { c: "THUNG24" }],
  ]],

  ["Chăm sóc gia đình/Nước giặt/Nước giặt cửa trước", "CHAI", "THUNG4", [
    ["OMO-MATIC-CT-2.7", "Nước giặt OMO Matic cửa trước 2,7kg", 168000], ["COMFORT-GIAT-CT-2.6", "Nước giặt Comfort cửa trước 2,6kg", 158000], ["SURF-CT-2.4", "Nước giặt Surf cửa trước 2,4kg", 118000],
    ["ARIEL-CT-2.4", "Nước giặt Ariel cửa trước 2,4kg", 192000, { x: 1 }],
  ]],
  ["Chăm sóc gia đình/Nước giặt/Nước giặt cửa trên", "CHAI", "THUNG4", [
    ["OMO-MATIC-CTR-2.7", "Nước giặt OMO Matic cửa trên 2,7kg", 162000], ["SUNLIGHT-GIAT-3.6", "Nước giặt Sunlight cửa trên 3,6kg", 135000], ["ATTACK-3.6", "Nước giặt Attack 3,6kg", 148000],
    ["ABA-3.2", "Nước giặt Aba 3,2kg", 128000],
  ]],
  ["Chăm sóc gia đình/Nước xả vải", "CHAI", "THUNG6", [
    ["COMFORT-DIUDANG-3.2", "Nước xả vải Comfort đậm đặc 3,2 lít", 155000], ["COMFORT-TUI-1.6", "Nước xả vải Comfort túi 1,6 lít", 78000, { u: "TUI", c: "THUNG8" }],
    ["DOWNY-NANGMAI-3", "Nước xả vải Downy nắng mai 3 lít", 148000], ["LENOR-2.5", "Nước xả vải Lenor 2,5 lít", 128000],
  ]],
  ["Chăm sóc gia đình/Nước rửa chén", "CHAI", "THUNG12", [
    ["SUNLIGHT-CHEN-750", "Nước rửa chén Sunlight chanh 750ml", 38500], ["SUNLIGHT-CHEN-3.6", "Nước rửa chén Sunlight chanh 3,6 kg", 128000, { u: "CAN", c: "THUNG4" }],
    ["MYKINH-CHEN-750", "Nước rửa chén Mỹ Hảo 750ml", 32500], ["LIX-CHEN-750", "Nước rửa chén Lix trà xanh 750ml", 34500, { x: 1 }], ["SUNLIGHT-TUI-600", "Nước rửa chén Sunlight túi 600ml", 24500, { u: "TUI", c: "THUNG20" }],
  ]],
  ["Chăm sóc gia đình/Nước lau sàn", "CHAI", "THUNG12", [
    ["SUNLIGHT-LAUSAN-1KG", "Nước lau sàn Sunlight 1kg", 42500], ["GIFT-LAUSAN-1KG", "Nước lau sàn Gift 1kg", 38500], ["LIX-LAUSAN-1KG", "Nước lau sàn Lix hương hoa 1kg", 36500],
    ["VIM-LAUSAN-1KG", "Nước lau sàn Vim 1kg", 44500],
  ]],
  ["Chăm sóc gia đình/Chất tẩy rửa", "CHAI", "THUNG12", [
    ["VIM-TAYBON-880", "Nước tẩy bồn cầu Vim 880ml", 38500], ["HARPIC-CAU-750", "Nước tẩy bồn cầu Harpic 750ml", 52500], ["JAVEL-MR-1L", "Nước tẩy Javel Mr Muscle 1 lít", 28500],
    ["CIF-KEM-690", "Kem tẩy đa năng Cif 690g", 48500], ["LIX-TAYNHA-900", "Nước tẩy rửa nhà tắm Lix 900ml", 32500],
  ]],
  ["Chăm sóc gia đình/Túi rác", "CUON", "THUNG20", [
    ["TUIRAC-NHO-50", "Túi rác đen cuộn nhỏ 50 cái", 15500], ["TUIRAC-TRUNG-30", "Túi rác đen cuộn trung 30 cái", 19500], ["TUIRAC-LON-20", "Túi rác đen cuộn lớn 20 cái", 24500],
    ["TUIRAC-SINHHOC-20", "Túi rác tự phân hủy sinh học cuộn 20 cái", 32500],
  ]],

  ["Giấy và sản phẩm vệ sinh/Giấy vệ sinh", "LOC", "THUNG12", [
    ["BLESSYOU-10CUON", "Giấy vệ sinh Bless You 10 cuộn", 52000], ["BLESSYOU-4CUON", "Giấy vệ sinh Bless You 4 cuộn", 22000], ["PULPPY-10CUON", "Giấy vệ sinh Pulppy 10 cuộn", 48000],
    ["SAIGON-GVS-10", "Giấy vệ sinh Sài Gòn 10 cuộn", 38000], ["ELLEMME-GVS-10", "Giấy vệ sinh Elleme 10 cuộn", 58000, { x: 1 }], ["WATERSILK-GVS-12", "Giấy vệ sinh Watersilk 12 cuộn", 68000],
  ]],
  ["Giấy và sản phẩm vệ sinh/Khăn giấy", "GOI", "THUNG30", [
    ["PULPPY-KG-RUT", "Khăn giấy rút Pulppy 180 tờ", 9500], ["SAIGON-KG-RUT", "Khăn giấy rút Sài Gòn 200 tờ", 8500], ["BLESSYOU-KG-HOP", "Khăn giấy hộp Bless You 180 tờ", 22500, { u: "HOP", c: "THUNG24" }],
    ["WATERSILK-KG-RUT", "Khăn giấy rút Watersilk 3 lớp", 14500],
  ]],
  ["Giấy và sản phẩm vệ sinh/Khăn ướt", "GOI", "THUNG48", [
    ["BOBBY-KU-80", "Khăn ướt Bobby 80 tờ", 28500], ["MAMAMY-KU-100", "Khăn ướt Mamamy 100 tờ", 24500], ["BIORE-KU-20", "Khăn ướt Bioré 20 tờ", 12500], ["WATERWIPES-KU-60", "Khăn ướt WaterWipes 60 tờ", 98000, { c: "THUNG24" }],
  ]],
  ["Giấy và sản phẩm vệ sinh/Giấy ăn", "GOI", "THUNG48", [
    ["PULPPY-GA-NAPKIN", "Giấy ăn vuông Pulppy 100 tờ", 7500], ["SAIGON-GA-NAPKIN", "Giấy ăn vuông Sài Gòn 100 tờ", 6800], ["NAPKIN-NHA-HANG", "Giấy ăn nhà hàng Hoa Mai 500 tờ", 16500, { c: "THUNG20" }],
  ]],
  ["Giấy và sản phẩm vệ sinh/Băng vệ sinh", "GOI", "THUNG48", [
    ["DIANA-SENSI-8", "Băng vệ sinh Diana Sensi cool fresh 8 miếng", 14500], ["LAURIER-8", "Băng vệ sinh Laurier siêu mỏng cánh 8 miếng", 16500], ["KOTEX-STYLE-8", "Băng vệ sinh Kotex Style 8 miếng", 15500],
    ["CAREFREE-BVS-20", "Băng hằng ngày Carefree 20 miếng", 18500],
  ]],

  ["Đồ dùng cá nhân/Bàn chải đánh răng", "CAI", "THUNG72", [
    ["PS-BANCHAI-MEM", "Bàn chải P/S lông mềm", 12500], ["COLGATE-SLIM-SOFT", "Bàn chải Colgate Slim Soft", 28500], ["ORALB-CLASSIC", "Bàn chải Oral-B Classic", 24500], ["SENSODYNE-BANCHAI", "Bàn chải Sensodyne mềm", 42500, { x: 1 }],
  ]],
  ["Đồ dùng cá nhân/Dao cạo", "CAI", "THUNG48", [
    ["GILLETTE-BLUE2", "Dao cạo râu Gillette Blue II", 18500], ["GILLETTE-VECTOR3", "Dao cạo râu Gillette Vector 3", 38500], ["SCHICK-XTREME3", "Dao cạo râu Schick Xtreme 3", 36500],
  ]],
  ["Đồ dùng cá nhân/Khăn tắm và khăn mặt", "CAI", "THUNG20", [
    ["KHANMAT-COTTON-30X50", "Khăn mặt cotton 30x50cm", 18500], ["KHANTAM-COTTON-60X120", "Khăn tắm cotton 60x120cm", 68500], ["KHANMAT-TRE-30X30", "Khăn mặt sợi tre 30x30cm", 14500],
  ]],
  ["Đồ dùng cá nhân/Vệ sinh cá nhân", "CAI", "THUNG48", [
    ["TAMBONG-GOI-100", "Tăm bông ngoáy tai hộp 100 que", 8500, { u: "HOP", c: "THUNG48" }], ["LUOC-NHUA", "Lược nhựa chải tóc", 7500], ["BAMMONG-INOX", "Bấm móng tay inox", 14500], ["GIAYTHAM-DAU-50", "Giấy thấm dầu hộp 50 tờ", 18500, { u: "HOP", c: "THUNG48" }],
  ]],

  ["Gia vị và nguyên liệu nấu ăn/Nước mắm", "CHAI", "THUNG12", [
    ["NAMNGU-500", "Nước mắm Nam Ngư chai 500ml", 36500], ["NAMNGU-750", "Nước mắm Nam Ngư chai 750ml", 52500], ["CHINSU-500", "Nước mắm Chinsu chai 500ml", 34500],
    ["PHUQUOC-ONG-500", "Nước mắm Phú Quốc Khải Hoàn 500ml", 68500], ["HUNGTHINH-500", "Nước mắm cá cơm Hưng Thịnh 500ml", 42500, { x: 1 }],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Nước tương", "CHAI", "THUNG12", [
    ["MAGGI-TUONG-300", "Nước tương Maggi đậu nành 300ml", 21500], ["CHINSU-TUONG-500", "Nước tương Chinsu 500ml", 22500], ["TAMTHAI-TUONG-500", "Nước tương Tam Thái Tử 500ml", 18500],
    ["KIKKOMAN-TUONG-250", "Nước tương Kikkoman 250ml", 38500, { c: "THUNG24" }],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Dầu ăn", "CHAI", "THUNG12", [
    ["NEPTUNE-1L", "Dầu ăn Neptune 1 lít", 58500], ["SIMPLY-1L", "Dầu ăn Simply đậu nành 1 lít", 52500], ["TUONGAN-1L", "Dầu ăn Tường An 1 lít", 49500], ["MARVELA-1L", "Dầu ăn Marvela 1 lít", 46500],
    ["NEPTUNE-5L", "Dầu ăn Neptune can 5 lít", 275000, { u: "CAN", c: "THUNG4" }], ["SIMPLY-2L", "Dầu ăn Simply chai 2 lít", 102000, { c: "THUNG8" }],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Muối", "GOI", "THUNG40", [
    ["MUOI-IOT-500", "Muối i-ốt Bạc Liêu 500g", 4500], ["MUOI-TINH-1KG", "Muối tinh Việt Nam 1kg", 7500], ["MUOI-TOM-250", "Muối tôm Tây Ninh 250g", 12500, { x: 1 }], ["MUOI-HONG-HIMALAYA-500", "Muối hồng Himalaya 500g", 38500],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Đường", "KG", "THUNG20", [
    ["DUONG-BIENHOA-1KG", "Đường tinh luyện Biên Hòa 1kg", 24500, { u: "BICH" }], ["DUONG-LAGI-1KG", "Đường trắng Lam Sơn 1kg", 21500, { u: "BICH" }], ["DUONG-PHEN-500", "Đường phèn Biên Hòa 500g", 22500, { u: "BICH" }],
    ["DUONG-CATTAY-1KG", "Đường cát vàng 1kg", 23500, { u: "BICH" }],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Bột ngọt", "GOI", "THUNG20", [
    ["AJINOMOTO-400", "Bột ngọt Ajinomoto gói 400g", 38500], ["AJINOMOTO-1KG", "Bột ngọt Ajinomoto gói 1kg", 88500, { c: "THUNG10" }], ["VEDAN-400", "Bột ngọt Vedan gói 400g", 33500], ["MIWON-400", "Bột ngọt Miwon gói 400g", 34500],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Hạt nêm", "GOI", "THUNG20", [
    ["KNORR-HATNEM-900", "Hạt nêm Knorr thịt thăn xương ống 900g", 62500], ["KNORR-HATNEM-400", "Hạt nêm Knorr 400g", 31500], ["MAGGI-HATNEM-900", "Hạt nêm Maggi 900g", 58500], ["CHINSU-HATNEM-400", "Hạt nêm Chinsu 400g", 29500],
    ["AJI-NGON-HATNEM-1KG", "Hạt nêm Aji-ngon 1kg", 72500, { x: 1 }],
  ]],
  ["Gia vị và nguyên liệu nấu ăn/Tương ớt", "CHAI", "THUNG24", [
    ["CHINSU-TUONGOT-250", "Tương ớt Chinsu chai 250g", 12500], ["CHINSU-TUONGOT-500", "Tương ớt Chinsu chai 500g", 21500, { c: "THUNG12" }], ["CHOLIMEX-TUONGOT-270", "Tương ớt Cholimex 270g", 13500],
    ["HEINZ-CA-300", "Tương cà Heinz chai 300g", 28500], ["MAGGI-TUONGCA-250", "Tương cà Maggi chai 250g", 15500],
  ]],

  ["Đồ ăn nhẹ/Snack khoai và bắp", "GOI", "THUNG24", [
    ["OISHI-BAPNGOT-42", "Snack Oishi bắp ngọt 42g", 4800], ["LAYS-TUMUC-52", "Snack Lay's khoai tây vị tự nhiên 52g", 11500], ["POCA-KHOAI-54", "Snack Poca khoai tây vị phô mai 54g", 8500], ["OSTAR-KHOAIT-42", "Snack O'Star khoai tây 42g", 7800],
    ["PRINGLES-107", "Snack Pringles hộp 107g", 36500, { u: "HOP", c: "THUNG12" }],
  ]],
  ["Đồ ăn nhẹ/Rong biển", "GOI", "THUNG48", [
    ["ONLY-RONGBIEN-5", "Rong biển ăn liền Only Seaweed 5g", 5500], ["TAOKAENOI-RONGBIEN-32", "Rong biển Tao Kae Noi 32g", 17500], ["HANSOL-RONGBIEN-10", "Rong biển sấy Hansol 10g", 8500, { x: 1 }],
  ]],
  ["Đồ ăn nhẹ/Hạt các loại", "GOI", "THUNG20", [
    ["HATDIEU-RANGMUOI-500", "Hạt điều rang muối Bình Phước 500g", 128000], ["HATHUONGDUONG-200", "Hạt hướng dương Chacheer 130g", 18500], ["HATDE-SAY-200", "Hạt dẻ cười Mỹ 200g", 88500], ["HATBI-300", "Hạt bí rang 300g", 48500],
  ]],
  ["Đồ ăn nhẹ/Khô chế biến", "GOI", "THUNG20", [
    ["KHOBO-MIENG-100", "Khô bò miếng Vissan 100g", 78500], ["KHAGA-LALIEU-100", "Khô gà lá chanh 100g", 52500], ["KHOMUC-NUONG-100", "Khô mực nướng xé sợi 100g", 98500], ["THITHEO-SAY-100", "Thịt heo sấy 100g", 62500],
  ]],
  ["Đồ ăn nhẹ/Đậu phộng", "GOI", "THUNG30", [
    ["OHAYO-DAUPHONG-100", "Đậu phộng rang Ohayo 100g", 12500], ["TANTAN-DAUPHONG-80", "Đậu phộng da cá Tân Tân 80g", 9500], ["DAUPHONG-TOI-100", "Đậu phộng rang tỏi ớt 100g", 11500],
  ]],
  ["Đồ ăn nhẹ/Bắp rang", "GOI", "THUNG30", [
    ["POPCORN-BOT-90", "Bắp rang bơ Hàn Quốc 90g", 14500], ["POPCORN-CARAMEN-90", "Bắp rang caramel 90g", 15500], ["POPCORN-LAMNONG-100", "Bắp rang vị phô mai 100g", 14500],
  ]],

  ["Cà phê và trà/Cà phê hòa tan/Cà phê 3 trong 1", "HOP", "THUNG24", [
    ["NESCAFE-3IN1-20", "Cà phê Nescafé 3 trong 1 hộp 20 gói", 52500], ["G7-3IN1-21", "Cà phê G7 3 trong 1 hộp 21 gói", 54500], ["VINACAFE-3IN1-24", "Cà phê Vinacafé gold 3 trong 1 hộp 24 gói", 49500],
    ["TRUNGNGUYEN-3IN1-18", "Cà phê Trung Nguyên Legend 3 trong 1 hộp 18 gói", 58500, { x: 1 }],
  ]],
  ["Cà phê và trà/Cà phê hòa tan/Cà phê đen và cappuccino", "HOP", "THUNG24", [
    ["NESCAFE-DEN-17", "Cà phê đen Nescafé hộp 17 gói", 48500], ["G7-DEN-15", "Cà phê đen G7 hộp 15 gói", 42500], ["CAPPUCCINO-NESCAFE-12", "Cà phê cappuccino Nescafé hộp 12 gói", 56500],
  ]],
  ["Cà phê và trà/Cà phê rang xay", "GOI", "THUNG20", [
    ["TRUNGNGUYEN-SANGTAO-500", "Cà phê rang xay Trung Nguyên Sáng tạo 5 gói 500g", 128000], ["HIGHLANDS-RANGXAY-200", "Cà phê rang xay Highlands 200g", 62500], ["CHON-RANGXAY-500", "Cà phê rang xay Chồn nguyên chất 500g", 98500],
  ]],
  ["Cà phê và trà/Trà túi lọc", "HOP", "THUNG48", [
    ["LIPTON-NHANVANG-25", "Trà Lipton nhãn vàng hộp 25 túi", 32500], ["DILMAH-VANG-25", "Trà Dilmah hộp 25 túi", 42500], ["COZY-DAO-25", "Trà túi lọc Cozy hương đào hộp 25 túi", 28500, { x: 1 }],
    ["GUNG-TUILOC-20", "Trà gừng túi lọc hộp 20 túi", 24500],
  ]],
  ["Cà phê và trà/Trà khô", "GOI", "THUNG20", [
    ["TRATHAINGUYEN-100", "Chè Thái Nguyên khô gói 100g", 38500], ["TRASEN-HANOI-100", "Trà sen Hà Nội 100g", 78500], ["TRAOLONG-200", "Trà ô long khô 200g", 88500],
  ]],
  ["Cà phê và trà/Trà hòa tan", "HOP", "THUNG24", [
    ["TRASUA-XANH-10", "Trà sữa hòa tan Maccaffe hộp 10 gói", 42500], ["TRADAO-HOATAN-10", "Trà đào hòa tan hộp 10 gói", 38500], ["TRAGUNG-HOATAN-10", "Trà gừng hòa tan hộp 10 gói", 34500],
  ]],

  ["Đồ dùng nhà bếp/Màng bọc thực phẩm", "CUON", "THUNG20", [
    ["MANGBOC-30X100", "Màng bọc thực phẩm PE 30cm x 100m", 48500], ["MANGBOC-30X30", "Màng bọc thực phẩm PE 30cm x 30m", 22500], ["MANGBOC-HOP-LON", "Màng bọc thực phẩm cuộn lớn 45cm x 100m", 72500],
  ]],
  ["Đồ dùng nhà bếp/Giấy bạc", "CUON", "THUNG20", [
    ["GIAYBAC-30X5", "Giấy bạc nướng 30cm x 5m", 22500], ["GIAYBAC-30X10", "Giấy bạc nướng 30cm x 10m", 38500], ["GIAYNUONG-30X10", "Giấy nến nướng 30cm x 10m", 28500],
  ]],
  ["Đồ dùng nhà bếp/Túi zip", "GOI", "THUNG48", [
    ["TUIZIP-S-20", "Túi zip nhỏ gói 20 cái", 12500], ["TUIZIP-M-20", "Túi zip trung gói 20 cái", 18500], ["TUIZIP-L-15", "Túi zip lớn gói 15 cái", 22500],
  ]],
  ["Đồ dùng nhà bếp/Miếng rửa chén", "CAI", "THUNG120", [
    ["SCOTCH-XANH-3", "Miếng rửa chén Scotch-Brite xanh", 6500], ["COSMOS-BOT-BIEN", "Miếng rửa chén bọt biển Cosmos", 4500], ["COSMOS-INOX-3", "Cọ rửa chén sợi inox bộ 3", 12500],
  ]],
  ["Đồ dùng nhà bếp/Găng tay", "VI", "THUNG24", [
    ["GANGTAY-CAOSU-M", "Găng tay cao su rửa chén size M", 18500, { u: "CAI" }], ["GANGTAY-NYLON-100", "Găng tay nilon dùng một lần 100 cái", 22500, { u: "HOP" }], ["GANGTAY-Y-TE-100", "Găng tay y tế latex hộp 100 cái", 78500, { u: "HOP" }],
  ]],
  ["Đồ dùng nhà bếp/Dụng cụ vệ sinh", "CAI", "THUNG24", [
    ["CHOIQUET-NHA", "Chổi quét nhà cán nhôm", 38500], ["CAY-LAU-NHA-360", "Cây lau nhà xoay 360 độ", 168000], ["XONHUA-12L", "Xô nhựa 12 lít có quai", 32500], ["HOT-RAC-NHUA", "Hót rác nhựa có cán", 18500],
  ]],

  ["Hàng tiêu dùng khác/Pin", "VI", "THUNG20", [
    ["PANASONIC-AA-4", "Pin Panasonic AA vỉ 4 viên", 32500], ["PANASONIC-AAA-4", "Pin Panasonic AAA vỉ 4 viên", 32500], ["ENERGIZER-AA-4", "Pin Energizer AA vỉ 4 viên", 58500], ["MAXELL-CR2032", "Pin cúc áo Maxell CR2032", 22500, { u: "CAI" }],
  ]],
  ["Hàng tiêu dùng khác/Bật lửa", "CAI", "THUNG50", [
    ["BATLUA-FLAMINGO", "Bật lửa Flamingo", 4500], ["BATLUA-ZIPPO-STD", "Bật lửa gas dùng nhiều lần", 12500], ["BATLUA-BEP", "Bật lửa bếp gas cán dài", 18500],
  ]],
  ["Hàng tiêu dùng khác/Khẩu trang", "HOP", "THUNG40", [
    ["KHAUTRANG-YTE-50", "Khẩu trang y tế 4 lớp hộp 50 cái", 28500], ["KHAUTRANG-N95-10", "Khẩu trang N95 hộp 10 cái", 85000], ["KHAUTRANG-VAI-3", "Khẩu trang vải kháng khuẩn gói 3 cái", 22500, { u: "GOI" }],
  ]],
  ["Hàng tiêu dùng khác/Đồ dùng tiện ích", "CAI", "THUNG24", [
    ["DEN-PIN-LED", "Đèn pin LED cầm tay", 38500], ["O-CAM-DIEN-3L", "Ổ cắm điện 3 lỗ dây 3m", 68500], ["BONG-LED-9W", "Bóng đèn LED 9W", 22500, { c: "THUNG50" }], ["BANG-KEO-TRONG", "Băng keo trong 5cm x 100 yard", 14500, { u: "CUON", c: "THUNG72" }],
  ]],

  ["Thực phẩm đông lạnh/Xúc xích", "GOI", "THUNG20", [
    ["VISSAN-XX-HEO-500", "Xúc xích heo Vissan gói 500g", 78500], ["CPFOODS-XX-GA-450", "Xúc xích gà CP gói 450g", 68500], ["DUCVIET-XX-TIETTRUNG-200", "Xúc xích Đức Việt tiệt trùng 200g", 38500],
    ["LEGEND-XX-XONGKHOI-175", "Xúc xích xông khói Legend gói 175g", 52500, { x: 1 }],
  ]],
  ["Thực phẩm đông lạnh/Chả cá", "GOI", "THUNG20", [
    ["CHACA-THATLAT-500", "Chả cá thát lát Vĩnh Hoàn 500g", 98500], ["CHACA-VIEN-500", "Cá viên chiên CP gói 500g", 62500], ["CHACA-HAINAM-300", "Chả cá Hải Nam gói 300g", 52500],
  ]],
  ["Thực phẩm đông lạnh/Viên thả lẩu", "GOI", "THUNG20", [
    ["TOMVIEN-CP-500", "Tôm viên CP gói 500g", 88500], ["BOVIEN-VISSAN-500", "Bò viên Vissan gói 500g", 82500], ["CAVIEN-TUOI-500", "Cá viên thả lẩu gói 500g", 58500], ["TAUHU-CA-300", "Tàu hũ cá thả lẩu gói 300g", 42500],
  ]],
  ["Thực phẩm đông lạnh/Thực phẩm chế biến sẵn", "GOI", "THUNG20", [
    ["NUGGET-GA-CP-400", "Gà viên chiên Nugget CP gói 400g", 72500], ["GIOCHA-DONGLANH-500", "Giò chả đông lạnh 500g", 68500], ["BANHBAO-NHANTHIT-300", "Bánh bao nhân thịt gói 300g", 38500], ["HAKAO-TOM-300", "Há cảo tôm gói 300g", 58500],
  ]],

  ["Đồ dùng cho trẻ em/Tã, bỉm", "GOI", "THUNG4", [
    ["BOBBY-M-60", "Tã dán Bobby size M 60 miếng", 165000], ["BOBBY-L-54", "Tã dán Bobby size L 54 miếng", 165000], ["PAMPERS-M-52", "Tã dán Pampers size M 52 miếng", 248000], ["HUGGIES-L-50", "Tã quần Huggies size L 50 miếng", 235000],
    ["MOONY-NB-60", "Tã dán Moony size NB 60 miếng", 275000, { x: 1 }],
  ]],
  ["Đồ dùng cho trẻ em/Khăn ướt trẻ em", "GOI", "THUNG24", [
    ["BOBBY-KUTE-100", "Khăn ướt trẻ em Bobby 100 tờ", 28500], ["PAMPERS-KUTE-80", "Khăn ướt trẻ em Pampers 80 tờ", 42500], ["ANTOAN-KUTE-50", "Khăn ướt trẻ em An Toàn 50 tờ", 18500],
  ]],
  ["Đồ dùng cho trẻ em/Sữa trẻ em", "HOP", "THUNG6", [
    ["DIELAC-ALPHA-380", "Sữa bột Dielac Alpha Gold 2 hộp 380g", 118000], ["ENFAGROW-3-870", "Sữa bột Enfagrow A+ số 3 hộp 870g", 565000], ["NAN-OPTIPRO-4-800", "Sữa bột Nan Optipro số 4 hộp 800g", 465000],
    ["FRISO-GOLD-3-900", "Sữa bột Friso Gold số 3 hộp 900g", 535000], ["ABBOTT-GROW-900", "Sữa bột Similac Gold số 4 hộp 900g", 545000, { x: 1 }],
  ]],
  ["Đồ dùng cho trẻ em/Đồ ăn dặm", "GOI", "THUNG24", [
    ["BOTAN-DAM-GAO-200", "Bột ăn dặm Nestlé Cerelac gạo sữa 200g", 62500], ["NGUCOC-ANDAM-MIX-250", "Ngũ cốc ăn dặm Gerber 250g", 98500], ["CHAO-TUOI-SAMYANG", "Cháo tươi ăn dặm Cây Thị gói 240g", 21500], ["BANHAN-DAM-GERBER", "Bánh ăn dặm Gerber 35g", 52500],
  ]],
  ["Đồ dùng cho trẻ em/Chăm sóc trẻ em", "CHAI", "THUNG12", [
    ["JOHNSON-TAMGOI-500", "Sữa tắm gội Johnson's Baby 500ml", 98500], ["LACTACYD-BABY-250", "Sữa tắm Lactacyd Baby 250ml", 88500], ["JOHNSON-PHAN-200", "Phấn rôm Johnson's Baby 200g", 38500, { u: "HOP" }],
    ["BEPANTHEN-30", "Kem chống hăm Bepanthen tuýp 30g", 128000, { u: "TUYP", c: "THUNG24" }],
  ]],

  ["Thức ăn thú cưng/Thức ăn cho chó", "GOI", "THUNG12", [
    ["SMARTHEART-BO-1.5", "Thức ăn chó SmartHeart vị bò 1,5kg", 118000], ["PEDIGREE-BO-400", "Thức ăn chó Pedigree vị bò 400g", 52500], ["ANF-CHO-3KG", "Thức ăn hạt cho chó Ganador 3kg", 268000, { u: "BAO", c: "THUNG4" }],
  ]],
  ["Thức ăn thú cưng/Thức ăn cho mèo", "GOI", "THUNG12", [
    ["WHISKAS-CA-400", "Thức ăn mèo Whiskas vị cá thu 400g", 58500], ["ME-O-GOLD-1.2", "Thức ăn mèo Me-O Gold 1,2kg", 138000], ["CATSRANG-3KG", "Thức ăn hạt mèo Cat's Eye 3kg", 215000, { u: "BAO", c: "THUNG4" }],
  ]],
];

module.exports = { NEW_UNITS, TREE, BLOCKS };
