public class TestHash {
    public static void main(String[] args) {
        String addr1 = "Thôn Ân Phú Xã Phú Lâm Huyện Tiên Du Tỉnh Bắc Ninh";
        long hash1 = 0;
        for (int i = 0; i < addr1.length(); i++) {
            hash1 = 31 * hash1 + addr1.charAt(i);
        }
        System.out.println("Addr1: " + (1.0 + (Math.abs(hash1) % 490) / 10.0));

        String addr2 = "Vinhomes Smart City Tây Mỗ, Phường Tây Mỗ, Quận Nam Từ Liêm, Thành phố Hà Nội";
        long hash2 = 0;
        for (int i = 0; i < addr2.length(); i++) {
            hash2 = 31 * hash2 + addr2.charAt(i);
        }
        System.out.println("Addr2: " + (1.0 + (Math.abs(hash2) % 490) / 10.0));
    }
}
