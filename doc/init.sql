create database es_compat;
GRANT ALL PRIVILEGES ON es_compat.* TO 'es_compat'@'%' IDENTIFIED BY 'es_compat_passwd';
flush privileges;
use es_compat;
CREATE TABLE `index_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `index` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `dest_index` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remote_cluster_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int(11) DEFAULT NULL,
  `es_type` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uni_index` (`index`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

