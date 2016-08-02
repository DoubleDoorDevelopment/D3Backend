-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

-- -----------------------------------------------------
-- Schema backend
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema backend
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `backend` DEFAULT CHARACTER SET utf8 ;
USE `backend` ;

-- -----------------------------------------------------
-- Table `backend`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `backend`.`user` (
  `Username` VARCHAR(32) NOT NULL,
  `Password` VARCHAR(32) NULL,
  `Salt` VARCHAR(50) NULL,
  `SteamUser` VARCHAR(32) NULL,
  `SteamPass` VARCHAR(32) NULL,
  `SteamSalt` VARCHAR(50) NULL,
  `Group` ENUM('User', 'Admin') NULL,
  PRIMARY KEY (`Username`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `backend`.`server`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `backend`.`server` (
  `Name` VARCHAR(45) NOT NULL,
  `User` VARCHAR(45) NULL,
  `Port` VARCHAR(5) NULL,
  `Purpose` ENUM('Minecraft', 'Factorio', 'Starbound') NULL,
  INDEX `UsernameLink_idx` (`User` ASC),
  PRIMARY KEY (`Name`),
  CONSTRAINT `ServerToUserLink`
    FOREIGN KEY (`User`)
    REFERENCES `backend`.`user` (`Username`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `backend`.`moderator`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `backend`.`moderator` (
  `Identifier` INT NOT NULL AUTO_INCREMENT,
  `Owner` VARCHAR(45) NOT NULL,
  `Server` VARCHAR(45) NOT NULL,
  `User` VARCHAR(45) NOT NULL,
  `Permissions` ENUM('Moderator', 'Admin') NOT NULL,
  PRIMARY KEY (`Identifier`),
  INDEX `ModToServerLink_idx` (`Server` ASC),
  INDEX `ModToUserLink_idx` (`User` ASC),
  INDEX `ModToOwnerLink_idx` (`Owner` ASC),
  CONSTRAINT `ModToOwnerLink`
    FOREIGN KEY (`Owner`)
    REFERENCES `backend`.`user` (`Username`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ModToServerLink`
    FOREIGN KEY (`Server`)
    REFERENCES `backend`.`server` (`Name`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `ModToUserLink`
    FOREIGN KEY (`User`)
    REFERENCES `backend`.`user` (`Username`)
    ON DELETE CASCADE
    ON UPDATE CASCADE)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
