CREATE TABLE IF NOT EXISTS `user` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `Name` varchar(100) NOT NULL,
  `syncsts` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`Id`)
)