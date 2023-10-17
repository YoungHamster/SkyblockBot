DROP TABLE IF EXISTS `average_item_prices`;

DROP TABLE IF EXISTS `compact_ah_no_enchants`;
CREATE TABLE `compact_ah_no_enchants` (
  `id` int NOT NULL AUTO_INCREMENT,
  `timestamp` bigint NOT NULL,
  `price` int NOT NULL,
  `bin` bit NOT NULL,
  `productId` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS `bazaar_historic_prices`;
CREATE TABLE `compact_ah_no_enchants` (
  `id` int NOT NULL AUTO_INCREMENT,
  `timestamp` bigint NOT NULL,
  `price` int NOT NULL,
  `tradeVol24h` int NOT NULL,
  `productId` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;