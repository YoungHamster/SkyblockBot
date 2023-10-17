DROP TABLE IF EXISTS `bazaar_historic_prices`;
CREATE TABLE `bazaar_historic_prices` (
  `id` int NOT NULL AUTO_INCREMENT,
  `timestamp` bigint NOT NULL,
  `price` int NOT NULL,
  `tradeVol24h` int NOT NULL,
  `productId` varchar(100) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;