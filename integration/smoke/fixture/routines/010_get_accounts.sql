CREATE PROCEDURE dbo.GetActiveAccounts
AS
BEGIN
    SELECT *
    FROM dbo.Accounts a
    INNER JOIN dbo.Users u
        ON u.Id = a.UserId
    WHERE u.IsActive = 1;
END;
GO

CREATE FUNCTION dbo.GetUserById(@UserId INT)
RETURNS TABLE
AS
RETURN
(
    SELECT Id, Username, Email
    FROM dbo.Users
    WHERE Id = @UserId
);
GO
