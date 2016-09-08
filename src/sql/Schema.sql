
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `admin_log`
--

DROP TABLE IF EXISTS `admin_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `admin_log` (
  `requestid` varchar(255) NOT NULL,
  `userid` varchar(255) NOT NULL,
  `created` bigint(20) NOT NULL,
  `connectionid` varchar(255) NOT NULL,
  `sessionid` varchar(255) NOT NULL,
  `api` varchar(255) NOT NULL,
  `request` text,
  PRIMARY KEY (`requestid`),
  KEY `UserIndex` (`userid`),
  KEY `CreatedIndex` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `adminsession`
--

DROP TABLE IF EXISTS `adminsession`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `adminsession` (
  `id` varchar(255) NOT NULL,
  `userid` varchar(255) NOT NULL,
  `started` bigint(20) NOT NULL,
  `lastactive` bigint(20) NOT NULL,
  `expired` tinyint(4) NOT NULL,
  `connected` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `LastActiveIndex` (`lastactive`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `adminsession_userid`
--

DROP TABLE IF EXISTS `adminsession_userid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `adminsession_userid` (
  `userid` varchar(255) NOT NULL,
  `sessionid` varchar(255) NOT NULL,
  KEY `UserIndex` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agent_key`
--

DROP TABLE IF EXISTS `agent_key`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agent_key` (
  `key` varchar(255) NOT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `iprange` varchar(45) DEFAULT NULL,
  `expiration` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currency`
--

DROP TABLE IF EXISTS `currency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency` (
  `userid` varchar(255) NOT NULL,
  `currency` varchar(255) NOT NULL,
  `balance` int(11) NOT NULL,
  `remainder` double NOT NULL DEFAULT '0',
  `modified` bigint(20) NOT NULL,
  PRIMARY KEY (`userid`,`currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currency_history`
--

DROP TABLE IF EXISTS `currency_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency_history` (
  `userid` varchar(255) NOT NULL,
  `currency` varchar(255) NOT NULL,
  `before` int(11) NOT NULL,
  `delta` int(11) NOT NULL,
  `after` int(11) NOT NULL,
  `action` varchar(255) NOT NULL,
  `modified` bigint(20) NOT NULL,
  KEY `UserIndex` (`userid`,`modified`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currency_info`
--

DROP TABLE IF EXISTS `currency_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency_info` (
  `id` varchar(255) NOT NULL,
  `starting` int(11) NOT NULL DEFAULT '0',
  `min` int(11) DEFAULT NULL,
  `max` int(11) DEFAULT NULL,
  `rate` double NOT NULL DEFAULT '0',
  `history` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invite`
--

DROP TABLE IF EXISTS `invite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `invite` (
  `id` varchar(255) NOT NULL,
  `created` bigint(20) NOT NULL,
  `created_by` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `keydata`
--

DROP TABLE IF EXISTS `keydata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `keydata` (
  `id` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `owner` varchar(255) NOT NULL,
  `parent` varchar(255) DEFAULT NULL,
  `version` int(11) NOT NULL,
  `created` bigint(20) NOT NULL,
  `modified` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Type_index` (`type`),
  KEY `Parent_index` (`parent`),
  KEY `Owner_index` (`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `keydata_item`
--

DROP TABLE IF EXISTS `keydata_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `keydata_item` (
  `id` varchar(255) NOT NULL,
  `key` varchar(255) CHARACTER SET ascii NOT NULL,
  `data` varbinary(62000) DEFAULT NULL,
  `datatype` int(11) NOT NULL,
  `created` bigint(20) NOT NULL,
  `modified` bigint(20) NOT NULL,
  `deleted` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`,`key`),
  KEY `ModifiedTime` (`modified`),
  KEY `DeletedIndex` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `script`
--

DROP TABLE IF EXISTS `script`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `script` (
  `id` varchar(255) NOT NULL,
  `source` longtext NOT NULL,
  `created` bigint(20) NOT NULL,
  `modified` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` varchar(255) NOT NULL,
  `sessionid` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `passwdhash` varchar(255) DEFAULT NULL,
  `lang` varchar(255) DEFAULT NULL,
  `country` varchar(255) DEFAULT NULL,
  `created` bigint(20) NOT NULL,
  `modified` bigint(20) NOT NULL,
  `admin` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_email`
--

DROP TABLE IF EXISTS `user_email`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_email` (
  `email` varchar(255) NOT NULL,
  `id` varchar(255) DEFAULT NULL,
  `hold` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_session`
--

DROP TABLE IF EXISTS `user_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_session` (
  `id` varchar(255) NOT NULL,
  `userid` varchar(255) NOT NULL,
  `started` bigint(20) NOT NULL,
  `lastactive` bigint(20) NOT NULL,
  `expired` tinyint(4) NOT NULL,
  `connected` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `LastActiveIndex` (`lastactive`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_session_userid`
--

DROP TABLE IF EXISTS `user_session_userid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_session_userid` (
  `userid` varchar(255) NOT NULL,
  `sessionid` varchar(255) NOT NULL,
  KEY `UserIndex` (`userid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_username`
--

DROP TABLE IF EXISTS `user_username`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_username` (
  `username` varchar(255) NOT NULL,
  `id` varchar(255) DEFAULT NULL,
  `hold` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-07-01  9:25:53
