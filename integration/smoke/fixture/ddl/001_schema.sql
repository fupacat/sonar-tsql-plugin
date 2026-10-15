CREATE TABLE dbo.Users (
    Id INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL,
    Email NVARCHAR(255) NOT NULL,
    IsActive BIT NOT NULL DEFAULT 1
);

CREATE TABLE dbo.Accounts (
    AccountId INT NOT NULL PRIMARY KEY,
    UserId INT NOT NULL,
    Balance DECIMAL(18,2) NOT NULL DEFAULT 0,
    CONSTRAINT FK_Accounts_Users FOREIGN KEY (UserId) REFERENCES dbo.Users (Id)
);

CREATE INDEX IX_Accounts_UserId ON dbo.Accounts (UserId);

CREATE TABLE dbo.WideValues (
    WideValue BIGINT NOT NULL PRIMARY KEY
);
